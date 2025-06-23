import {SyntaxNode} from "@lezer/common";

// path should be token ids
type TokenID = number;
export function nodesWithPath(root: SyntaxNode, path: TokenID[]) : SyntaxNode[] {
    return nodesWithPathImpl([root], path, 0);
}

function nodesWithPathImpl(from: SyntaxNode[], path: TokenID[], index: number) : SyntaxNode[] {
    if (index >= path.length) {
        return from;
    } else {
        let next = from.flatMap((node) => node.getChildren(path[index]));
        return nodesWithPathImpl(next, path, index + 1)
    }
}

export function climbTill(from: SyntaxNode, till: TokenID): SyntaxNode | null {
    let at: SyntaxNode | null = from;
    while (at != null && at.type.id != till) {
        at = at.parent;
    }
    return at;
}
