import { Tree } from "@lezer/common";
import { CompletionContext } from "@codemirror/autocomplete";
import * as tokens from "./generated/typeql.grammar.generated.terms";
import {nodesWithPath} from "./navigation";
import {
    Schema,
    SchemaAttribute,
    SchemaRole,
    SchemaEntity,
    SchemaRelation,
    SchemaConcept
} from "../../service/schema-state.service";
type SchemaObject = SchemaEntity | SchemaRelation;

type TypeLabel = string;

function extractText(text: string, from: number, to: number): string {
    return text.slice(from, to);
}

function labels(types: SchemaConcept[]): string[] {
    return types.map(type => type.label);
}

export class TypeQLAutocompleteSchema {
    fromDB: Schema;
    fromEditor: Schema;

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
        return Object.values(this.fromDB.attributes).concat(Object.values(this.fromEditor.attributes));
    }

    objectTypes(): SchemaObject[] {
        return (this.entityTypes() as SchemaObject[]).concat(this.relationTypes() as SchemaObject[]);
    }

    entityTypes(): SchemaEntity[] {
        return Object.values(this.fromDB.entities)
            .concat(Object.values(this.fromEditor.entities));
    }

    relationTypes(): SchemaRelation[] {
        return Object.values(this.fromDB.relations).concat(Object.values(this.fromEditor.relations));
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
        return objectType ? objectType.playableRoles : [];
    }
    getRelates(label: TypeLabel): SchemaRole[] {
        const objectType = this.objectType(label);
        return objectType ? objectType.playableRoles : [];
    }

    // static fromTypeQL(text: string, tree: Tree) : TypeQLAutocompleteSchemaImpl {
    //     let builder = new SchemaBuilder();
    //     // TODO: Replace iterate with a more targetted traversal that considers only define queries.
    //     // Extract all type declarations from the tree
    //     let root = tree.topNode;
    //     let definitionTypes = nodesWithPath(root, [tokens.QuerySchema, tokens.QueryDefine, tokens.Definables, tokens.Definable, tokens.DefinitionType])
    //     definitionTypes.forEach(node => {
    //         let kindNode = node.getChild(tokens.KIND);
    //         let labelNode = node.getChild(tokens.LABEL);
    //         if (kindNode != null && labelNode != null) {
    //             let kind = extractText(text, kindNode.from, kindNode.to);
    //             let label = extractText(text, labelNode.from, labelNode.to);
    //             switch (kind) {
    //                 case "entity": {
    //                     builder.objectType(label);
    //                     break;
    //                 }
    //                 case "relation": {
    //                     builder.objectType(label);
    //                     break;
    //                 }
    //                 case "attribute": {
    //                     builder.attributeType(label);
    //                     break;
    //                 }
    //             }
    //         }
    //     })
    //
    //
    //     // Extract owns/relates/plays. Idk what to do with sub or annotations.
    //     definitionTypes.forEach(node => {
    //         let labelNode = node.getChild(tokens.LABEL);
    //         if (labelNode == null) return;
    //         let label = extractText(text, labelNode.from, labelNode.to);
    //         nodesWithPath(node, [tokens.TypeCapability, tokens.TypeCapabilityBase])
    //             .map(typeCapabilityBaseNode => typeCapabilityBaseNode.firstChild)
    //             .forEach(actualCapabilityNode => {
    //                 switch (actualCapabilityNode?.type.id) {
    //                     // We actually only want type-declarations for now.
    //                     case tokens.RelatesDeclaration: {
    //                         let roleTypeNode = actualCapabilityNode.firstChild!.nextSibling!;
    //                         let roleType = extractText(text, roleTypeNode.from, roleTypeNode.to);
    //                         builder.recordRelates(label, `${label}:${roleType}`);
    //                         break;
    //                     }
    //                     default: {
    //                         // Ignore other capabilities for now
    //                         break;
    //                     }
    //                 }
    //             });
    //     });
    //     return builder.build();
    // }
}

//
// export class SchemaBuilder {
//     objectTypes: Record<TypeLabel, SchemaObject>;
//     attributes: Record<TypeLabel, SchemaAttribute>;
//
//     constructor() {
//         this.objectTypes = {};
//         this.attributes = {};
//     }
//
//     attributeType(type: TypeLabel): SchemaAttribute {
//         if (!this.attributes[type]) {
//             this.attributes[type] = { owners: [] };
//         }
//         return this.attributes[type];
//     }
//
//     objectType(type: TypeLabel): SchemaObject {
//         if (!this.objectTypes[type]) {
//             this.objectTypes[type] = { owns: [], plays: [], relates: [] };
//         }
//         return this.objectTypes[type];
//     }
//
//     recordOwns(type: TypeLabel, ownedType: TypeLabel): void {
//         const objectType = this.objectType(type);
//         if (!objectType.owns.includes(ownedType)) {
//             objectType.owns.push(ownedType);
//         }
//     }
//     recordPlays(type: TypeLabel, playedType: TypeLabel): void {
//         const objectType = this.objectType(type);
//         if (!objectType.plays.includes(playedType)) {
//             objectType.plays.push(playedType);
//         }
//     }
//
//     recordRelates(type: TypeLabel, relatedType: TypeLabel): void {
//         const objectType = this.relationType(type);
//         if (!objectType.roleplayers.includes(relatedType)) {
//             objectType.roleplayers.push(relatedType);
//         }
//     }
//
//     build(): TypeQLAutocompleteSchemaImpl {
//         return new TypeQLAutocompleteSchemaImpl(this.objectTypes, this.attributes);
//     }
// }


function buildSchemafromTypeQL(text: string, tree: Tree) : Schema {
    // TODO;
    return {entities: {}, relations: {}, attributes: {}};
}
