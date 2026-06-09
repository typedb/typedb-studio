/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Graph } from "./graph";

/** External store of attribute values per instance IID, populated by
 *  label-only fetches that don't push to the graph. Used as a second source
 *  for the label heuristic alongside in-graph attribute neighbors. */
export type DisplayAttributeStore = Map<string, Map<string, unknown>>;

/**
 * Re-derive labels for every entity / relation instance in the graph.
 *
 * Two-phase, so each *type* picks one display attribute that every instance
 * of that type uses:
 *  1. Walk every entity/relation node. Group their available attribute type
 *     labels (union over in-graph `has` neighbors *and* the off-graph
 *     display-attribute store) by the instance's type label.
 *  2. Score each candidate attribute type label per group and pick the
 *     highest-scoring one as that instance type's display attribute. Then
 *     apply it: each instance's label becomes `<typeLabel>: <value>` if it
 *     has a value for the chosen attribute, otherwise just `<typeLabel>`.
 *
 * Picking one attribute per *type* is what keeps labels consistent across
 * siblings — without it, two `person` nodes could end up labelled with
 * different attributes (one `name`, one `id`) just because their loaded
 * attribute sets differ.
 */
export function refreshInstanceLabels(graph: Graph, store?: DisplayAttributeStore): void {
    // Phase 1: collect candidate attribute type labels per instance type.
    const candidatesByType = new Map<string, Set<string>>();
    graph.forEachNode((nodeKey, attrs) => {
        const concept = attrs.metadata?.concept as any;
        if (!concept) return;
        if (concept.kind !== "entity" && concept.kind !== "relation") return;
        const typeLabel: string | undefined = concept.type?.label;
        if (!typeLabel) return;
        let set = candidatesByType.get(typeLabel);
        if (!set) { set = new Set(); candidatesByType.set(typeLabel, set); }
        graph.forEachOutNeighbor(nodeKey, (neighborKey: string) => {
            const n = graph.getNodeAttributes(neighborKey);
            const nc = n?.metadata?.concept as any;
            const tl = nc?.kind === "attribute" ? nc.type?.label : null;
            if (tl) set!.add(tl);
        });
        if (store && concept.iid) {
            const perOwner = store.get(concept.iid);
            if (perOwner) for (const tl of perOwner.keys()) set.add(tl);
        }
    });

    // Phase 2: pick the highest-scoring attribute type per instance type.
    const chosenByType = new Map<string, string | null>();
    for (const [typeLabel, candidates] of candidatesByType.entries()) {
        let best: { typeLabel: string; score: number } | null = null;
        for (const candTypeLabel of candidates) {
            const score = scoreAttributeTypeName(candTypeLabel);
            if (score <= 0) continue;
            if (!best || score > best.score) best = { typeLabel: candTypeLabel, score };
        }
        chosenByType.set(typeLabel, best?.typeLabel ?? null);
    }

    // Phase 3: apply.
    graph.forEachNode((nodeKey, attrs) => {
        const concept = attrs.metadata?.concept as any;
        if (!concept) return;
        if (concept.kind !== "entity" && concept.kind !== "relation") return;
        const typeLabel: string = concept.type?.label ?? "";
        const chosen = chosenByType.get(typeLabel) ?? null;
        const value = chosen != null
            ? lookupAttributeValue(graph, nodeKey, concept.iid, chosen, store)
            : undefined;
        const newLabel = value != null && String(value).length > 0
            ? `${typeLabel}: ${formatValue(value)}`
            : typeLabel;
        if (attrs.label !== newLabel) {
            graph.setNodeAttribute(nodeKey, "label", newLabel);
        }
    });
}

function lookupAttributeValue(
    graph: Graph,
    ownerKey: string,
    ownerIid: string | undefined,
    chosenTypeLabel: string,
    store?: DisplayAttributeStore,
): unknown {
    // Prefer in-graph value when the user has explicitly loaded the
    // attribute (so what they see matches what's drawn). Fall back to the
    // off-graph store.
    let found: unknown = undefined;
    graph.forEachOutNeighbor(ownerKey, (neighborKey: string) => {
        if (found !== undefined) return;
        const n = graph.getNodeAttributes(neighborKey);
        const nc = n?.metadata?.concept as any;
        if (nc?.kind === "attribute" && nc.type?.label === chosenTypeLabel) {
            found = nc.value;
        }
    });
    if (found !== undefined) return found;
    if (store && ownerIid) return store.get(ownerIid)?.get(chosenTypeLabel);
    return undefined;
}

const NAME_TIER_A = new Set([
    "name", "title", "label",
    "fullname", "displayname", "givenname", "firstname", "lastname",
    "username", "handle", "alias",
    "nickname", "screenname",
]);

// Tier between A and B: "text" is content-y but not a canonical "name". We
// want it to win against id/email/code yet still lose to name/title/label.
const NAME_TIER_A2 = new Set([
    "text",
]);

const NAME_TIER_B = new Set([
    "email", "emailaddress",
    "identifier", "id", "uid", "uuid",
    "code", "slug", "key", "ref", "reference",
]);

/**
 * Score an attribute type name purely on how name-like it is. Higher is
 * better; 0 means "don't use this attribute". Tiers:
 *  - A   (~300): canonical name fields
 *  - A2  (~250): `text`
 *  - B   (~200): identifier-ish (email, id, code, ...)
 *  - C   (~100): anything containing "name", "title" or "text"
 *  - D   (~50):  fallback for any unknown attribute name
 * Within a tier, shorter type labels win (so `name` beats `display_name`).
 */
function scoreAttributeTypeName(typeLabel: string): number {
    if (!typeLabel) return 0;
    const norm = typeLabel.toLowerCase().replace(/[-_\s]/g, "");
    const lengthPenalty = Math.min(typeLabel.length, 40);
    if (NAME_TIER_A.has(norm)) return 300 - lengthPenalty;
    if (NAME_TIER_A2.has(norm)) return 250 - lengthPenalty;
    if (NAME_TIER_B.has(norm)) return 200 - lengthPenalty;
    if (norm.includes("name") || norm.includes("title") || norm.includes("text")) {
        return 100 - lengthPenalty;
    }
    return 50 - lengthPenalty;
}

function formatValue(value: unknown): string {
    if (typeof value === "string") return value;
    if (value == null) return "";
    return String(value);
}
