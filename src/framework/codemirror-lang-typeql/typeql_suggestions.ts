import * as tokens from "./generated/typeql.grammar.generated.terms";
import { CompletionContext, Completion } from "@codemirror/autocomplete";
import {SyntaxNode, NodeType, Tree} from "@lezer/common"
import { SuggestionMap, SuffixOfPrefixSuggestion, suggest } from "./complete";
import { TypeQLAutocompleteSchema } from "./typeQLAutocompleteSchema";
import {climbTill, nodesWithPath} from "./navigation";

function findIsaConstraintLabelsForVar(context: CompletionContext, parseAt: SyntaxNode): string[] {
    let parentStatementThing = climbTill(parseAt, tokens.StatementThing)!;
    let varNode = parentStatementThing.getChild(tokens.VAR)!;
    let varName = context.state.sliceDoc(varNode.from, varNode.to);
    let pipelineNode = climbTill(parentStatementThing, tokens.Pipeline)!;
    // TODO: Maybe consider disjunctions?
    let statements = nodesWithPath(pipelineNode, [tokens.QueryStage, tokens.ClauseMatch, tokens.Patterns, tokens.Pattern, tokens.Statement, tokens.StatementThing]);
    let relevantStatementThings = statements
        .filter(n => {
            let otherVarNode = n.getChild(tokens.VAR)!;
            return context.state.sliceDoc(otherVarNode.from, otherVarNode.to) == varName;
        });
    let possibleLabels = relevantStatementThings
        .flatMap(n => nodesWithPath(n, [tokens.ThingConstraintList, tokens.ThingConstraint, tokens.IsaConstraint, tokens.TypeRef]))
        .map(n => context.state.sliceDoc(n.from, n.to));
    // console.log(parentStatementThing, varNode, varName, pipelineNode, statements, relevantStatementThings, possibleLabels);
    return possibleLabels;
}

function suggestAttributeTypeForHas(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    let possibleLabels = findIsaConstraintLabelsForVar(context, parseAt);
    if (possibleLabels.length > 0) {
        return possibleLabels.flatMap(owner => schema.getOwns(owner))
            .map((label) => suggest("AttributeType", label))
    } else {
        return suggestAttributeTypeLabels(context, tree, parseAt, climbedTo, prefix, schema);
    }
}

function suggestRoleTypeForLinks(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    let possibleLabels = findIsaConstraintLabelsForVar(context, parseAt);
    if (possibleLabels.length > 0) {
        return possibleLabels.flatMap(owner => schema.getRelates(owner))
            .map((label) => suggest("RoleType", label.split(":")[1]))
    } else {
        return suggestRoleTypeLabelsUnscoped(context, tree, parseAt, climbedTo, prefix, schema);
    }
}

function suggestAttributeTypeLabels(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    return schema.attributeTypes().map((label) => { return suggest("AttributeType", label); });
}

function suggestObjectTypeLabels(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    var options: Completion[] = [];
    schema.objectTypes().forEach((label) => {options.push(suggest("ObjectType", label));})
    return options;
}

function suggestRoleTypeLabelsScoped(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    var options: Completion[] = [];
    schema.objectTypes()
        .flatMap((label) => schema.objectType(label).relates)
        .forEach((label) => { options.push(suggest("RoleType", label));});
    return options;
}

function suggestRoleTypeLabelsUnscoped(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    var options: Completion[] = [];
    schema.objectTypes()
        .flatMap((label) => schema.objectType(label).relates)
        .forEach((label) => { options.push(suggest("RoleType", label.split(":")[1]));});    return options;
}

function suggestThingTypeLabels(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    return suggestAttributeTypeLabels(context, tree, parseAt, climbedTo, prefix, schema).concat(
        suggestObjectTypeLabels(context, tree, parseAt, climbedTo, prefix, schema)
    );
}

function suggestVariables(context: CompletionContext, tree: Tree, boost= 0): Completion[] {
    var options: Completion[] = [];
    tree.iterate({
        enter: (other: SyntaxNode) => {
            if (other.type.id == tokens.VAR) {
                let varName = context.state.sliceDoc(other.from, other.to);
                options.push(suggest("variable", varName, boost));
            }
        }
    });
    return options;
}

function suggestVariablesAt10(context: CompletionContext, tree: Tree): Completion[] {
    return suggestVariables(context, tree, 10);
}

function suggestVariablesAtMinus10(context: CompletionContext, tree: Tree): Completion[] {
    return suggestVariables(context, tree, -10);
}

function suggestThingConstraintKeywords(): Completion[] {
    return ["isa", "has", "links"].map((constraintName) => {
        return {
            label: constraintName,
            type: "thingConstraint",
            apply: constraintName,
            info: "Thing constraint keyword",
        };
    });
}
function suggestTypeConstraintKeywords(): Completion[] {
    return ["sub", "owns", "relates", "plays"].map((constraintName) => {
        return {
            label: constraintName,
            type: "typeConstraint",
            apply: constraintName,
            info: "Type constraint keyword",
        };
    });
}

function suggestDefinedKeywords(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    return ["define", "redefine", "undefine"].map((keyword) => suggest("keyword", keyword, 1));
}

function suggestPipelineStages(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    return ["match", "insert", "delete", "update", "put", "select", "reduce", "sort", "limit", "offset", "end"].map((keyword) => suggest("keyword", keyword, 1))
}

function suggestKinds(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    return ["entity", "attribute", "relation"].map((keyword) => suggest("kind", keyword, 2));
}

function suggestNestedPatterns(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], schema: TypeQLAutocompleteSchema): Completion[] {
    return ["not {};", "{} or {};", "try {};"].map((keyword) => suggest("method", keyword, 2));
}

const SUFFIX_VAR_OR_COMMA = [[tokens.COMMA], [tokens.VAR]];


// Will pick the first matching suffix. If you want to handle things manually, use an empty suffix.
const SUGGESTION_GROUP_FOR_THING_STATEMENTS: SuffixOfPrefixSuggestion<TypeQLAutocompleteSchema>[]  = [
        { suffixes: SUFFIX_VAR_OR_COMMA, suggestions: [suggestThingConstraintKeywords] },
        { suffixes: [[tokens.HAS]], suggestions: [suggestAttributeTypeForHas, suggestVariablesAtMinus10] },
        { suffixes: [[tokens.ISA]], suggestions: [suggestThingTypeLabels, suggestVariablesAtMinus10] },
        { suffixes: [[tokens.HAS, tokens.TypeRef], [tokens.ISA, tokens.TypeRef]], suggestions: [suggestVariablesAtMinus10] },
];

export const SUGGESTION_MAP: SuggestionMap<TypeQLAutocompleteSchema> = {
    [tokens.LABEL]: [{ suffixes: [[]], suggestions: [suggestThingTypeLabels] }],
    [tokens.VAR]: [{ suffixes: [[]], suggestions: [suggestVariablesAt10] }],
    [tokens.Relation]: [
        {suffixes: [[tokens.PARENOPEN], [tokens.COMMA]], suggestions: [suggestRoleTypeForLinks, suggestVariablesAtMinus10]},
        {suffixes: [[tokens.COLON]], suggestions: [suggestVariablesAt10]},
    ],
    [tokens.Statement]: [
        { suffixes: SUFFIX_VAR_OR_COMMA, suggestions: [suggestThingConstraintKeywords, suggestTypeConstraintKeywords] },
        { suffixes: [[tokens.HAS]], suggestions: [suggestAttributeTypeForHas, suggestVariablesAtMinus10] },
        { suffixes: [[tokens.ISA]], suggestions: [suggestThingTypeLabels, suggestVariablesAtMinus10] },
        { suffixes: [[tokens.HAS, tokens.TypeRef], [tokens.ISA, tokens.TypeRef]], suggestions: [suggestVariablesAtMinus10] },
        { suffixes: [[tokens.SEMICOLON, tokens.TypeRef]], suggestions: [suggestTypeConstraintKeywords] },
        { suffixes: [[tokens.OWNS]], suggestions: [suggestAttributeTypeLabels, suggestVariablesAtMinus10] },
        { suffixes: [[tokens.SUB]], suggestions: [suggestThingTypeLabels, suggestVariablesAtMinus10] },
        { suffixes: [[tokens.RELATES]], suggestions: [suggestRoleTypeLabelsUnscoped] },
        { suffixes: [[tokens.PLAYS]], suggestions: [suggestRoleTypeLabelsScoped] },
    ],
    [tokens.ClauseMatch]: [
        { suffixes: [[tokens.MATCH, tokens.TypeRef]], suggestions: [suggestTypeConstraintKeywords] },
        { suffixes: [[tokens.MATCH]], suggestions: [suggestNestedPatterns, suggestVariablesAt10, suggestThingTypeLabels ] },
    ],
    [tokens.ClauseInsert]: SUGGESTION_GROUP_FOR_THING_STATEMENTS,
    [tokens.Query]: [
        { suffixes: [[tokens.QuerySchema]], suggestions: [suggestThingTypeLabels, suggestKinds] },
        { suffixes: [[tokens.QueryPipelinePreambled]], suggestions: [suggestNestedPatterns, suggestVariablesAt10, suggestPipelineStages, ] },
    ],
    
    // Now some for define statements
    [tokens.QuerySchema]: [
        { suffixes: [[tokens.DEFINE]], suggestions: [ suggestThingTypeLabels, suggestKinds] },
        { suffixes: [[tokens.DEFINE, tokens.LABEL]], suggestions: [ suggestTypeConstraintKeywords] }
    ],
    [tokens.Definable]: [
        { suffixes: [[tokens.COMMA], [tokens.KIND, tokens.LABEL]], suggestions: [ suggestTypeConstraintKeywords ] },
        { suffixes: [[tokens.OWNS]], suggestions: [suggestAttributeTypeLabels] },
        { suffixes: [[tokens.SUB]], suggestions: [suggestThingTypeLabels] },
        { suffixes: [ [tokens.PLAYS] ], suggestions: [ suggestRoleTypeLabelsScoped ] },
        { suffixes: [ [tokens.RELATES] ], suggestions: [ suggestRoleTypeLabelsUnscoped ] },
    ],
    // TODO: ...
};
