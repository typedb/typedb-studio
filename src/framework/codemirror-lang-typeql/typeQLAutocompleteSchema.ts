import { Tree } from "@lezer/common";
import { CompletionContext } from "@codemirror/autocomplete";
import * as tokens from "./generated/typeql.grammar.generated.terms";
import {nodesWithPath} from "./navigation";

type TypeLabel = string;
type AttributeTypeEntry = {};
interface ObjectTypeEntry {
    owns: TypeLabel[];
    plays: TypeLabel[];
    relates: TypeLabel[];
}

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
        let root = tree.topNode;
        let definitionTypes = nodesWithPath(root, [tokens.QuerySchema, tokens.QueryDefine, tokens.Definables, tokens.Definable, tokens.DefinitionType])
        definitionTypes.forEach(node => {
            let kindNode = node.getChild(tokens.KIND);
            let labelNode = node.getChild(tokens.LABEL);
            if (kindNode != null && labelNode != null) {
                let kind = extractText(text, kindNode.from, kindNode.to);
                let label = extractText(text, labelNode.from, labelNode.to);
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
        })


        // Extract owns/relates/plays. Idk what to do with sub or annotations.
        definitionTypes.forEach(node => {
            let labelNode = node.getChild(tokens.LABEL);
            if (labelNode == null) return;
            let label = extractText(text, labelNode.from, labelNode.to);
            nodesWithPath(node, [tokens.TypeCapability, tokens.TypeCapabilityBase])
                .map(typeCapabilityBaseNode => typeCapabilityBaseNode.firstChild)
                .forEach(actualCapabilityNode => {
                    switch (actualCapabilityNode?.type.id) {
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
        });
        return builder.build();
    }
}

export class SchemaBuilder {
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
