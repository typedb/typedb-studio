/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { forceSimulation, forceLink, forceManyBody, forceCollide, forceCenter, forceX, forceY, SimulationNodeDatum, SimulationLinkDatum } from "d3-force";

/** Pure d3-force simulation setup, shared by the main-thread layout wrapper and
 *  the layout web worker — everything here must stay free of DOM/graphology. */

export interface SimNode extends SimulationNodeDatum {
    id: string;
    radius: number;
}
export type SimLink = SimulationLinkDatum<SimNode>;

export interface ForceSimOptions {
    /** Initial alpha (default 1.0). Lower = less perturbation. */
    initialAlpha?: number;
    /** Alpha decay rate (default 0.01). Higher = settles faster. */
    alphaDecay?: number;
    /** Combined centering-gravity multiplier (density preset × per-start option). */
    gravityMultiplier: number;
}

/**
 * Early-stop thresholds for the force sim. d3 keeps ticking until alpha decays
 * to alphaMin, but alpha decays exponentially — so the run has a long tail
 * where every node is effectively stationary. Instead we stop once the
 * fastest-moving node has been below SETTLE_MAX_VELOCITY (graph units / tick)
 * for SETTLE_QUIET_TICKS consecutive ticks; the consecutive-tick guard avoids
 * stopping during a brief lull early in the run when forces momentarily cancel.
 */
export const SETTLE_MAX_VELOCITY = 0.3;
export const SETTLE_QUIET_TICKS = 5;

/** Alpha target the collide-only drag sim is held at while dragging — keeps it
 *  warm so the cursor-fixed node resolves overlaps; endDrag sets it back to 0. */
export const DRAG_ALPHA_TARGET = 0.3;

const SIM_DISPLAY_FPS = 30;

/** Display sync interval, scaled down for large graphs — each sync re-indexes
 *  and repaints the whole graph. */
export function simSyncIntervalMs(nodeCount: number): number {
    const fps = nodeCount > 4000 ? 8 : nodeCount > 1500 ? 15 : SIM_DISPLAY_FPS;
    return 1000 / fps;
}

export function buildForceSimulation(nodes: SimNode[], links: SimLink[], opts: ForceSimOptions) {
    const vertexCount = nodes.length;
    const edgeCount = links.length;
    const maxRadius = nodes.reduce((max, n) => Math.max(max, n.radius), 0);
    const baseCharge = -1500 * (1 + Math.log(1 + edgeCount / (vertexCount + 1)));

    // Detect connected components (islands) — multiple islands get stronger
    // gravity so they don't drift apart.
    const componentOf = new Int32Array(nodes.length).fill(-1);
    const adj: number[][] = nodes.map(() => []);
    links.forEach(l => {
        const s = l.source as number, t = l.target as number;
        adj[s].push(t);
        adj[t].push(s);
    });
    let componentCount = 0;
    for (let i = 0; i < nodes.length; i++) {
        if (componentOf[i] >= 0) continue;
        const id = componentCount++;
        const stack = [i];
        while (stack.length > 0) {
            const cur = stack.pop()!;
            if (componentOf[cur] >= 0) continue;
            componentOf[cur] = id;
            for (const nb of adj[cur]) {
                if (componentOf[nb] < 0) stack.push(nb);
            }
        }
    }

    const gravityStrength = (componentCount > 1 ? 0.06 : 0.02) * opts.gravityMultiplier;

    return forceSimulation(nodes)
        .force("charge", forceManyBody().strength(baseCharge))
        .force("link", forceLink(links).distance(maxRadius).strength(1))
        .force("collide", forceCollide<SimNode>().radius(maxRadius))
        .force("center", forceCenter(0, 0))
        .force("x", forceX(0).strength(gravityStrength))
        .force("y", forceY(0).strength(gravityStrength))
        .alpha(opts.initialAlpha ?? 1.0)
        .alphaDecay(opts.alphaDecay ?? 0.01)
        .stop();
}

/** Collision-avoidance-only sim used while dragging — see buildDragSimulation
 *  notes in layout.ts. */
export function buildCollideSimulation(nodes: SimNode[]) {
    return forceSimulation(nodes)
        .force("collide", forceCollide<SimNode>().radius(d => d.radius))
        .alphaDecay(0.05)
        .stop();
}

// Messages between the layout worker and the main-thread wrapper.

export interface LayoutWorkerRunMsg {
    type: "run";
    runId: number;
    nodes: SimNode[];
    /** Index pairs into `nodes`. */
    links: { source: number; target: number }[];
    opts: ForceSimOptions;
}
export interface LayoutWorkerStartDragMsg { type: "startDrag"; runId: number; nodes: SimNode[]; }
export interface LayoutWorkerEndDragMsg { type: "endDrag"; }
export interface LayoutWorkerFixNodeMsg { type: "fixNode"; id: string; x: number; y: number; }
export interface LayoutWorkerUnfixNodeMsg { type: "unfixNode"; id: string; }
export interface LayoutWorkerStopMsg { type: "stop"; }
export type LayoutWorkerRequest = LayoutWorkerRunMsg | LayoutWorkerStartDragMsg | LayoutWorkerEndDragMsg
    | LayoutWorkerFixNodeMsg | LayoutWorkerUnfixNodeMsg | LayoutWorkerStopMsg;

export interface LayoutWorkerReadyMsg { type: "ready"; }
export interface LayoutWorkerPositionsMsg {
    type: "positions";
    runId: number;
    /** [x0, y0, x1, y1, ...] aligned with the run's `nodes` order. */
    positions: Float32Array;
    settled: boolean;
}
export type LayoutWorkerResponse = LayoutWorkerReadyMsg | LayoutWorkerPositionsMsg;
