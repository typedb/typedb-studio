import { Tree } from "@lezer/common";
import { CompletionContext } from "@codemirror/autocomplete";
import * as tokens from "./generated/typeql.grammar.generated.terms";
import {nodesWithPath} from "./navigation";
import {
    Schema,
    SchemaAttribute,
    SchemaRole,
    SchemaEntity,
    SchemaRelation
} from "../../service/schema-state.service";
type SchemaObject = SchemaEntity | SchemaRelation;

type TypeLabel = string;

function extractText(text: string, from: number, to: number): string {
    return text.slice(from, to);
}

export class TypeQLAutocompleteSchema {
    fromDB: Schema;
    fromEditor: Schema; // Partial because we don't care about subtypes, supertype or valueType

    constructor(fromDB: Schema, fromEditor: Schema) {
        this.fromDB = fromDB;
        this.fromEditor = fromEditor;
    }

    updateFromDB(fromDB: Schema): void {
        this.fromDB = fromDB;
    }

    mayUpdateFromEditorState(context: CompletionContext, tree: Tree): void {
        this.fromEditor = buildSchemafromTypeQL(context.state.sliceDoc(), tree);
    }

    attributeTypes(): SchemaAttribute[] {
        return record_values(this.fromDB.attributes).concat(record_values(this.fromEditor.attributes));
    }

    objectTypes(): SchemaObject[] {
        return (this.entityTypes() as SchemaObject[]).concat(this.relationTypes() as SchemaObject[]);
    }

    entityTypes(): SchemaEntity[] {
        return record_values(this.fromDB.entities)
            .concat(record_values(this.fromEditor.entities));
    }

    relationTypes(): SchemaRelation[] {
        return record_values(this.fromDB.relations).concat(record_values(this.fromEditor.relations));
    }

    attributeType(type: TypeLabel): SchemaAttribute {
        return this.fromDB.attributes[type] ??
            this.fromEditor.attributes[type];
    }

    objectType(type: TypeLabel): SchemaObject | null {
        return this.entityType(type) ?? this.relationType(type);
    }

    entityType(type: TypeLabel): SchemaEntity | null {
        return this.fromDB.entities[type] ?? this.fromEditor.entities[type];
    }

    relationType(type: TypeLabel): SchemaRelation {
        return this.fromDB.relations[type] ?? this.fromEditor.relations[type];
    }

    getOwns(label: TypeLabel): SchemaAttribute[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.ownedAttributes : [];
    }

    getPlays(label: TypeLabel): SchemaRole[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.playedRoles : [];
    }
    getRelates(label: TypeLabel): SchemaRole[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.playedRoles : [];
    }
}

export class SchemaBuilder {
    schema: Schema;
    constructor() {
        this.schema = { entities: {}, relations:{}, attributes: {} };
    }

    entityType(label: TypeLabel): SchemaEntity {
        if (!this.schema.entities[label]) {
            this.schema.entities[label] = {
                kind: "entityType", label,  ownedAttributes: [], playedRoles: [],
                subtypes: [],
            };
        }
        return this.schema.entities[label];
    }

    relationType(label: TypeLabel): SchemaRelation {
        if (!this.schema.relations[label]) {
            this.schema.relations[label] = {
                kind: "relationType", label,  ownedAttributes: [], playedRoles: [], relatedRoles: [],
                subtypes: [],
            };
        }
        return this.schema.relations[label];
    }

    attributeType(label: TypeLabel): SchemaAttribute {
        if (!this.schema.attributes[label]) {
            this.schema.attributes[label] = {
                kind: "attributeType", label, valueType: "string",
                subtypes: [],
            };
        }
        return this.schema.attributes[label];
    }

    getObjectType(label: TypeLabel): SchemaObject | null{
        return this.schema.entities[label] ?? this.schema.relations[label];
    }

    recordOwns(type: TypeLabel, ownedType: TypeLabel): void {
        const objectType = this.getObjectType(type)!;
        const attributeType = this.attributeType(ownedType);
        if (!objectType.ownedAttributes.includes(attributeType)) {
            objectType.ownedAttributes.push(attributeType);
        }
    }
    recordPlays(type: TypeLabel, playedType: TypeLabel): void {
        const objectType = this.getObjectType(type)!;
        let roleType: SchemaRole = { kind: "roleType", label: playedType };
        if (!objectType.playedRoles.includes(roleType)) {
            objectType.playedRoles.push(roleType);
        }
    }

    recordRelates(type: TypeLabel, relatedType: TypeLabel): void {
        const objectType = this.relationType(type);
        let roleType: SchemaRole = { kind: "roleType", label: relatedType };
        if (!objectType.relatedRoles.includes(roleType)) {
            objectType.relatedRoles.push(roleType);
        }
    }

    build(): Schema {
        return this.schema;
    }
}



function buildSchemafromTypeQL(text: string, tree: Tree) : Schema {
    let builder = new SchemaBuilder();
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
                    builder.entityType(label);
                    break;
                }
                case "relation": {
                    builder.relationType(label);
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

function record_values<Y>(records: Record<string, Y>): Y[] {
    return Object.entries(records).map(([_, value]) => value);
}
