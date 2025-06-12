
import { CompletionContext, Completion, CompletionResult } from "@codemirror/autocomplete";
import { syntaxTree } from "@codemirror/language"
import { SyntaxNode, NodeType, Tree } from "@lezer/common"
import { SUGGESTION_MAP } from "./typeql_suggestions";

export interface SuggestionMap<STATE> { 
    [key: number]: SuffixOfPrefixSuggestion<STATE>[]
};


export type SuffixCandidate = number[]; // A SuffixCandidate 's' "matches" a prefix if prefix[-s.length:] == s 
export interface SuffixOfPrefixSuggestion<STATE> {
    suffixes: SuffixCandidate[], // If any of the  suffix candidates match, the suggestions will be used.
    suggestions: SuggestionFunction<STATE>[]
};

export type SuggestionFunction<STATE> = (context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], state: STATE) => Completion[] | null;

export function suggest(type: string, label: string, boost: number = 0): Completion {
    // type (docs): used to pick an icon to show for the completion. Icons are styled with a CSS class created by appending the type name to "cm-completionIcon-".
    return {
        label: label,
        type: type,
        apply: label,
        info: type,
        boost: boost,
    };
}

interface NodePrefixAutoCompleteState {
    mayUpdateFromEditorState(context: CompletionContext, tree: Tree): void;
}

// See: https://codemirror.net/examples/autocompletion/ and maybe the SQL / HTML Example there.
export class NodePrefixAutoComplete<STATE extends NodePrefixAutoCompleteState> {
    suggestionMap: SuggestionMap<STATE>;
    suggestorState: STATE;

    constructor(suggestionMap: SuggestionMap<STATE>, suggestorState: STATE) {
        // This is where we would set up the autocompletion, but we do it in the index.ts file.
        // See: https://codemirror.net/docs/ref/#autocomplete.autocompletion
        this.suggestionMap = suggestionMap;
        this.suggestorState = suggestorState;
    }

    getState(): STATE {
        return this.suggestorState;
    }

    autocomplete(context: CompletionContext): CompletionResult | null {
        let tree: Tree = syntaxTree(context.state);
        this.suggestorState.mayUpdateFromEditorState(context, tree);
        let currentNode: SyntaxNode = tree.resolveInner(context.pos, -1); // https://lezer.codemirror.net/docs/ref/#common.SyntaxNode
        let options = this.getSuggestions(context, tree, currentNode);
        if (options != null) {
            // And once we figure out, we have to create a list of completion objects
            // It may be worth changing the grammar to be able to do this more easily, rather than replicate the original TypeQL grammar.
            // https://codemirror.net/docs/ref/#autocomplete.Completion
            let from = findStartOfCompletion(context) + 1;
            return {
                from: from,
                options: options,
                // Docs: "regular expression that tells the extension that, as long as the updated input (the range between the result's from property and the completion point) matches that value, it can continue to use the list of completions."
                validFor: /^([\w\$]+)?$/
            }
        } else {
            return null;
        }
    }

    getSuggestions(context: CompletionContext, tree: Tree, parseAt: SyntaxNode): Completion[] | null {
        return this.climbTillWeRecogniseSomething(context, tree, parseAt, parseAt, collectPrecedingChildrenOf(context, parseAt));
    }

    
    climbTillWeRecogniseSomething(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode | null, prefix: NodeType[]): Completion[] | null {
        if (climbedTo == null) {
            // this.logInterestingStuff(context, tree, parseAt, climbedTo, prefix);
            return null;
        }
        let suggestionEither = this.suggestionMap[climbedTo.type.id];
        if (suggestionEither != null) {
            for (var sops of (suggestionEither as SuffixOfPrefixSuggestion<STATE>[])) {
                if (prefixHasAnyOfSuffixes(prefix, sops.suffixes)) {
                    return this.combineSuggestions(context, tree, parseAt, climbedTo, prefix, sops.suggestions);
                }
            }
            // None match? Fall through.
            // console.log("Fell through!!!: ", climbedTo.type.name, "with prefix", prefix);
        }
        let newPrefix = collectSiblingsOf(climbedTo).concat(prefix);
        return this.climbTillWeRecogniseSomething(context, tree, parseAt, climbedTo.parent, newPrefix);
    }


     combineSuggestions(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode, prefix: NodeType[], suggestionFunctions: SuggestionFunction<STATE>[]): Completion[] {
        let suggestions = suggestionFunctions.map((f) => {
            return f(context, tree, parseAt, climbedTo, prefix, this.suggestorState);
        }).reduce((acc, curr) => { 
            return (curr == null) ? acc : acc!.concat(curr);
        }, []);
        // console.log("Matched:", climbedTo.type.name, "with prefix", prefix, ". Suggestions:", suggestions);
        return suggestions!;
    }

    logInterestingStuff(context: CompletionContext, tree: Tree, parseAt: SyntaxNode, climbedTo: SyntaxNode | null, prefix: NodeType[]) {
        console.log("Current Node:", parseAt.name);
        console.log("ClimbedTo Node:", climbedTo?.name);

        let at: SyntaxNode | null = parseAt;
        let climbThrough = [];
        while (at != null && at.name != climbedTo?.name) {
            climbThrough.push(at.name);
            at = at.parent;
        }
        climbThrough.push(at?.name);
        console.log("Climbed through", climbThrough);
        console.log("Prefix:", prefix);
    }
}

function isPartOfWord(s: string): boolean {
    let matches = s.match(/^[A-Za-z0-9_\-\$]+/);
    return matches != null && matches.length > 0;
}

function findStartOfCompletion(context: CompletionContext): number {
    let str = context.state.doc.sliceString(0, context.pos);
    let at = context.pos - 1;
    while (at >= 0 && isPartOfWord(str.charAt(at))) {
        at -= 1;
    }
    return at;
}

function collectSiblingsOf(node: SyntaxNode): NodeType[] {
    let siblings = [];
    let prev: SyntaxNode | null = node;
    while (null != (prev = prev.prevSibling)) {
        siblings.push(prev.type);
    };
    return siblings.reverse();
}

function collectPrecedingChildrenOf(context: CompletionContext, node: SyntaxNode): NodeType[] {
    let lastChild = node.childBefore(context.pos);
    if (lastChild == null) {
        return [];
    }
    let precedingChildren = collectSiblingsOf(lastChild);
    precedingChildren.push(lastChild.type);
    return precedingChildren;
}

function prefixHasAnyOfSuffixes(prefix: NodeType[], suffixes: SuffixCandidate[]): boolean {
    for (let i = 0; i < suffixes.length; i++) {
        if (prefixHasSuffix(prefix, suffixes[i])) {
            return true;
        }
    }
    return false;
}

function prefixHasSuffix(prefix: NodeType[], suffix: number[]): boolean {
    if (prefix.length < suffix.length) {
        return false;
    }
    for (let i = 0; i < suffix.length; i++) {
        if (prefix[prefix.length - suffix.length + i].id != suffix[i]) {
            return false;
        }
    }
    return true;
}
