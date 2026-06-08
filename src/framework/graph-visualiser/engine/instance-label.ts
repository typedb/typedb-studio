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
 * Re-derive labels for every entity / relation instance in the graph based on
 * whichever of its currently-attached attributes is most name-like.
 *
 * Called after each graph-builder pass so newly-loaded attribute edges feed
 * back into the owner's display label. The output is `<typeLabel>: <value>` if
 * a name-like attribute is found, otherwise just `<typeLabel>` (unchanged).
 */
export function refreshInstanceLabels(graph: Graph, store?: DisplayAttributeStore): void {
    graph.forEachNode((nodeKey, attrs) => {
        const concept = attrs.metadata?.concept as any;
        if (!concept) return;
        if (concept.kind !== "entity" && concept.kind !== "relation") return;

        const typeLabel: string = concept.type?.label ?? "";
        const best = pickDisplayAttribute(graph, nodeKey, concept.iid, store);
        const newLabel = best != null
            ? `${typeLabel}: ${formatValue(best.value)}`
            : typeLabel;

        if (attrs.label !== newLabel) {
            graph.setNodeAttribute(nodeKey, "label", newLabel);
        }
    });
}

interface CandidateAttribute {
    typeLabel: string;
    value: unknown;
    score: number;
}

function pickDisplayAttribute(
    graph: Graph, ownerKey: string, ownerIid: string | undefined, store?: DisplayAttributeStore,
): CandidateAttribute | null {
    let best: CandidateAttribute | null = null;
    const consider = (typeLabel: string, value: unknown) => {
        if (!typeLabel) return;
        const score = scoreAttribute(typeLabel, value);
        if (score <= 0) return;
        if (!best || score > best.score) best = { typeLabel, value, score };
    };

    // 1. In-graph attribute neighbors (user-loaded attribute edges).
    // `has` edges are directed owner → attribute.
    graph.forEachOutNeighbor(ownerKey, (neighborKey: string) => {
        const neighborAttrs = graph.getNodeAttributes(neighborKey);
        const concept = neighborAttrs?.metadata?.concept as any;
        if (!concept || concept.kind !== "attribute") return;
        consider(concept.type?.label ?? "", concept.value);
    });

    // 2. Off-graph attribute values from the label-only fetch.
    if (store && ownerIid) {
        const perOwner = store.get(ownerIid);
        if (perOwner) {
            for (const [typeLabel, value] of perOwner.entries()) {
                consider(typeLabel, value);
            }
        }
    }
    return best;
}

const NAME_TIER_A = new Set([
    "name", "title", "label",
    "fullname", "displayname", "givenname", "firstname", "lastname",
    "username", "handle", "alias",
    "nickname", "screenname",
]);

const NAME_TIER_B = new Set([
    "text",
    "email", "emailaddress",
    "identifier", "id", "uid", "uuid",
    "code", "slug", "key", "ref", "reference",
]);

/**
 * Score how good `value` is as a display label for an instance.
 * Higher is better; 0 means don't use this attribute.
 *
 * Tiers reflect "how likely a human would call this the thing's name":
 *  - A (~300): canonical name fields
 *  - B (~200): identifier-ish fields (email, code, id)
 *  - C (~100): anything else that contains "name" or "title"
 *  - D (~50):  reasonably-short string/number values
 * Within a tier we prefer shorter, simpler type labels (so `name` beats
 * `display_name`) and penalise unwieldy values.
 */
function scoreAttribute(typeLabel: string, value: unknown): number {
    if (value == null) return 0;
    if (typeof value === "boolean") return 0;

    const valueStr = String(value);
    if (valueStr.length === 0) return 0;
    if (valueStr.length > 120) return 0;

    const norm = typeLabel.toLowerCase().replace(/[-_\s]/g, "");
    const lengthPenalty = Math.min(typeLabel.length, 40);
    const valuePenalty = valueStr.length > 64 ? 20 : 0;

    let tierBase = 0;
    if (NAME_TIER_A.has(norm)) tierBase = 300;
    else if (NAME_TIER_B.has(norm)) tierBase = 200;
    else if (norm.includes("name") || norm.includes("title")) tierBase = 100;
    else if (typeof value === "string" || typeof value === "number") tierBase = 50;
    else return 0;

    return tierBase - lengthPenalty - valuePenalty;
}

function formatValue(value: unknown): string {
    if (typeof value === "string") return value;
    if (value == null) return "";
    return String(value);
}
