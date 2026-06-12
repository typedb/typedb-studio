/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Graph } from "./graph";

/** External store of attribute values per instance IID, populated by
 *  label-only fetches that don't push to the graph. Used as a second source
 *  for the label heuristic alongside in-graph attribute neighbors. Values are
 *  stored as arrays so multi-valued attributes (an owner with several values
 *  of the same attribute type) render every value, not just the last one
 *  recorded. */
export type DisplayAttributeStore = Map<string, Map<string, unknown[]>>;

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
export function refreshInstanceLabels(
    graph: Graph,
    store?: DisplayAttributeStore,
    overridesByType?: Map<string, string>,
): void {
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

    // Phase 2: pick the chosen attribute type per instance type. User
    // overrides (set via the type-detail inspector) always win and bypass
    // the heuristic; otherwise we pick the highest-scoring candidate.
    const chosenByType = new Map<string, string | null>();
    for (const [typeLabel, candidates] of candidatesByType.entries()) {
        const override = overridesByType?.get(typeLabel);
        if (override) {
            chosenByType.set(typeLabel, override);
            continue;
        }
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
        const collected = chosen != null
            ? collectAttributeValues(graph, nodeKey, concept.iid, chosen, store)
            : { values: [], isBoolean: false };
        const formatted = formatValues(collected.values);
        let newLabel: string;
        if (formatted.length === 0) {
            newLabel = typeLabel;
        } else if (collected.isBoolean && chosen) {
            // A bare `true`/`false` is meaningless on its own, so name the
            // attribute: `<type>: <attr-type>=<value>`.
            newLabel = `${typeLabel}: ${chosen}=${formatted}`;
        } else {
            newLabel = `${typeLabel}: ${formatted}`;
        }
        if (attrs.label !== newLabel) {
            graph.setNodeAttribute(nodeKey, "label", newLabel);
        }
    });
}

function collectAttributeValues(
    graph: Graph,
    ownerKey: string,
    ownerIid: string | undefined,
    chosenTypeLabel: string,
    store?: DisplayAttributeStore,
): { values: unknown[]; isBoolean: boolean } {
    // Union over both sources so a multi-valued attribute renders every
    // value, regardless of whether some came from explicit in-graph loads
    // and others from the off-graph label-only fetch.
    const out: unknown[] = [];
    let isBoolean = false;
    graph.forEachOutNeighbor(ownerKey, (neighborKey: string) => {
        const n = graph.getNodeAttributes(neighborKey);
        const nc = n?.metadata?.concept as any;
        if (nc?.kind === "attribute" && nc.type?.label === chosenTypeLabel) {
            out.push(nc.value);
            if (nc.valueType === "boolean" || nc.type?.valueType === "boolean" || typeof nc.value === "boolean") {
                isBoolean = true;
            }
        }
    });
    if (store && ownerIid) {
        const fromStore = store.get(ownerIid)?.get(chosenTypeLabel);
        if (fromStore) {
            out.push(...fromStore);
            // Store values are untyped; fall back to the JS type of the value.
            if (fromStore.some(v => typeof v === "boolean")) isBoolean = true;
        }
    }
    return { values: out, isBoolean };
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

/** Render a collected value-set as one label fragment. Deduplicates by the
 *  rendered string (so identical values across the in-graph + store sources
 *  show once), sorts ascending using a numeric-aware comparator (`"2"`
 *  before `"10"`), and joins with commas. Empty strings and null values are
 *  dropped — a `name` with no value at all reduces back to just the type
 *  label rather than `<type>: , `. */
function formatValues(values: unknown[]): string {
    if (values.length === 0) return "";
    const seen = new Set<string>();
    const strs: string[] = [];
    for (const v of values) {
        if (v == null) continue;
        const s = typeof v === "string" ? v : String(v);
        if (s.length === 0) continue;
        if (seen.has(s)) continue;
        seen.add(s);
        strs.push(s);
    }
    if (strs.length === 0) return "";
    strs.sort((a, b) => a.localeCompare(b, undefined, { numeric: true, sensitivity: "base" }));
    return strs.join(", ");
}
