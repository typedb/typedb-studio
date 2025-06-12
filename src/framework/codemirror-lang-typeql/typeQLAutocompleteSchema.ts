import { SyntaxNode, Tree, TreeCursor } from "@lezer/common";
import { CompletionContext } from "@codemirror/autocomplete";
import * as tokens from "./generated/typeql.grammar.generated.terms";
import {TypeDBHttpDriver} from "../typedb-driver";
import {finalize} from "rxjs";
import {DriverState} from "../../service/driver-state.service";
import {ConceptRow, ConceptRowsQueryResponse} from "../typedb-driver/response";
import {Concept, EntityType, RelationType, Type} from "../typedb-driver/concept";
type ConceptRowAnswer = { involvedBlocks: number[]; data: ConceptRow };
type TypeLabel = string;
type AttributeTypeEntry = {};
interface ObjectTypeEntry {
    owns: TypeLabel[];
    plays: TypeLabel[];
    relates: TypeLabel[];
};

function extractText(text: string, from: number, to: number): string {
    return text.slice(from, to);
}

export class TypeQLAutocompleteSchema {
    fromDB: TypeQLAutocompleteSchemaImpl;
    fromEditor: TypeQLAutocompleteSchemaImpl;

    constructor() {
        this.fromDB = new TypeQLAutocompleteSchemaImpl({}, {});
        this.fromEditor = new TypeQLAutocompleteSchemaImpl({}, {});
    }

    updateFromDB(schema: TypeQLAutocompleteSchemaImpl): void {
        this.fromDB = schema;
    }
    
    mayUpdateFromEditorState(context: CompletionContext, tree: Tree): void {
        this.fromEditor = TypeQLAutocompleteSchemaImpl.fromTypeQL(context.state.sliceDoc(), tree);
    }

    attributeTypes(): TypeLabel[] {
        return Object.keys(this.fromDB.attributes).concat(Object.keys(this.fromEditor.attributes));
    }

    objectTypes(): TypeLabel[] {
        return Object.keys(this.fromDB.objectTypes).concat(Object.keys(this.fromEditor.objectTypes));
    }

    attributeType(type: TypeLabel): AttributeTypeEntry {
        if (this.fromDB.attributes[type]) {
            return this.fromDB.attributes[type];
        } else {
            return this.fromEditor.attributes[type];
        }
    }

    objectType(type: TypeLabel): ObjectTypeEntry {
        if (this.fromDB.objectTypes[type]) {
            return this.fromDB.objectTypes[type];
        } else {
            return this.fromEditor.objectTypes[type];
        }
    }

    getOwns(label: TypeLabel): TypeLabel[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.owns : [];
    }

    getPlays(label: TypeLabel): TypeLabel[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.plays : [];
    }
    getRelates(label: TypeLabel): TypeLabel[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.relates : [];
    }
}

export class TypeQLAutocompleteSchemaImpl {
    objectTypes: Record<TypeLabel, ObjectTypeEntry>;
    attributes: Record<TypeLabel, AttributeTypeEntry>;
    constructor(
        objectTypes: Record<TypeLabel, ObjectTypeEntry>,
        attributes: Record<TypeLabel, AttributeTypeEntry>,
    ) {
        this.attributes = attributes;
        this.objectTypes = objectTypes;
    }

    static fromTypeQL(text: string, tree: Tree) : TypeQLAutocompleteSchemaImpl {
        let builder = new SchemaBuilder();
        // TODO: Replace iterate with a more targetted traversal that considers only define queries.
        // Extract all type declarations from the tree
        tree.iterate({
            enter: (cursor: TreeCursor) => {
                let node = cursor.node;
                if (node.type?.id === tokens.DefinitionType) {
                    if (node.firstChild?.type?.id === tokens.KIND) {
                        let labelNode = node.firstChild!.nextSibling!;
                        let label = extractText(text, labelNode.from, labelNode.to);
                        let kind = extractText(text, node.firstChild!.from, node.firstChild!.to);
                        switch (kind) {
                            case "entity": {
                                builder.objectType(label);
                                 break;
                            }
                            case "relation": {
                                builder.objectType(label);
                                 break;
                            }
                            case "attribute": {
                                builder.attributeType(label);
                                 break;
                            }
                        }
                    }
                }
            }
        });

        // Extract owns/relates/plays. Idk what to do with sub or annotations.
        tree.iterate({
            enter: (cursor: TreeCursor) => {
                let node = cursor.node;
                if (node.type.id === tokens.DefinitionType) {
                    let labelNode = (node.firstChild?.type?.id === tokens.KIND) ? node.firstChild!.nextSibling! : node.firstChild!;
                    let label = extractText(text, labelNode.from, labelNode.to);
                    node.getChildren(tokens.TypeCapability).forEach((typeCapabilityBaseNode: SyntaxNode) => {
                        let actualCapabilityNode = typeCapabilityBaseNode.firstChild!.firstChild!;
                        switch (actualCapabilityNode.type.id) {
                            // We actually only want type-declarations for now.
                            case tokens.RelatesDeclaration: {
                                let roleTypeNode = actualCapabilityNode.firstChild!.nextSibling!;
                                let roleType = extractText(text, roleTypeNode.from, roleTypeNode.to);
                                builder.recordRelates(label, `${label}:${roleType}`);
                                break;
                            }
                            default: {
                                // Ignore other capabilities for now
                                break;
                            }
                        }
                    });
                }
            }
        });
        return builder.build();
    }

    static fromDriver(driver: DriverState, database: string): TypeQLAutocompleteSchemaImpl | null {
        function runQuery(driver: DriverState, database: string, query: string): ConceptRowAnswer[] {
            // todo: Help please alex
            return [
                {
                    involvedBlocks: [0],
                    data: { // Unioning them all makes it pass
                        ["owner"]: {kind: "entityType", label: "person"},
                        ["owned"]: {kind: "attributeType", label: "name", valueType: "string"},
                        ["relation"]: {kind: "relationType", label: "friendship"},
                        ["related"]: {kind: "roleType", label: "friend"},
                        ["player"]: {kind: "entityType", label: "person"},
                        ["played"]: {kind: "roleType", label: "friend"},
                    }
                }
            ];
        }
        let ownsQuery = "match $owner owns $owned;"
        let relatesQuery = "match $relation relates $related;";
        let playsQuery = "match $player plays $played";

        let ownsAnswers = runQuery(driver, database, ownsQuery); // TODO:
        let relatesAnswers = runQuery(driver, database, relatesQuery);
        let playsAnswers = runQuery(driver, database, playsQuery);

        let builder = new SchemaBuilder();
        ownsAnswers.forEach((answer) => {
            let data: ConceptRow = answer.data;
            let owner = (data["owner"] as Type).label;
            let owned = (data["owned"] as Type).label;
            builder.objectType(owner);
            builder.attributeType(owned);
            builder.recordOwns(owner, owned);
        });
        relatesAnswers.forEach((answer) => {
            let data: ConceptRow = answer.data;
            let relation = (data["relation"] as Type).label;
            let related = (data["related"] as Type).label;
            builder.objectType(relation);
            builder.recordRelates(relation, related)
        });
        playsAnswers.forEach((answer) => {
            let data: ConceptRow = answer.data;
            let player = (data["player"] as Type).label;
            let played = (data["played"] as Type).label;
            builder.objectType(player);
            builder.recordRelates(player, played)
        })
        return builder.build();
    }
}

class SchemaBuilder {
    objectTypes: Record<TypeLabel, ObjectTypeEntry>;
    attributes: Record<TypeLabel, AttributeTypeEntry>;
    
    constructor() {
        this.objectTypes = {};
        this.attributes = {};
    }

    attributeType(type: TypeLabel): AttributeTypeEntry {
        if (!this.attributes[type]) {
            this.attributes[type] = { owners: [] };
        }
        return this.attributes[type];
    }
    
    objectType(type: TypeLabel): ObjectTypeEntry {
        if (!this.objectTypes[type]) {
            this.objectTypes[type] = { owns: [], plays: [], relates: [] };
        }
        return this.objectTypes[type];
    }

    recordOwns(type: TypeLabel, ownedType: TypeLabel): void {
        const objectType = this.objectType(type);
        if (!objectType.owns.includes(ownedType)) {
            objectType.owns.push(ownedType);
        }
    }
    recordPlays(type: TypeLabel, playedType: TypeLabel): void {
        const objectType = this.objectType(type);
        if (!objectType.plays.includes(playedType)) {
            objectType.plays.push(playedType);
        }
    }
    recordRelates(type: TypeLabel, relatedType: TypeLabel): void {
        const objectType = this.objectType(type);
        if (!objectType.relates.includes(relatedType)) {
            objectType.relates.push(relatedType);
        }
    }

    build(): TypeQLAutocompleteSchemaImpl {
        return new TypeQLAutocompleteSchemaImpl(this.objectTypes, this.attributes);
    }
}
