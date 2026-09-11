/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import {
    buildCollideSimulation, buildForceSimulation, DRAG_ALPHA_TARGET, LayoutWorkerRequest, LayoutWorkerResponse,
    SETTLE_MAX_VELOCITY, SETTLE_QUIET_TICKS, SimNode, simSyncIntervalMs,
} from "./d3-force-common";

/** Runs the d3-force simulation off the main thread. Ticks are driven by a
 *  budgeted setInterval loop (workers have no rAF everywhere) and positions are
 *  posted back as a transferable Float32Array at a size-adaptive display rate. */

// Typed as `Worker` purely for postMessage/onmessage shapes — this file is
// compiled with the app's DOM lib, which lacks DedicatedWorkerGlobalScope.
const ctx = self as unknown as Worker;

const TICK_INTERVAL_MS = 16;
const TICK_BUDGET_MS = 12;

let sim: ReturnType<typeof buildForceSimulation> | null = null;
let simNodes: SimNode[] = [];
let nodeById = new Map<string, SimNode>();
let runId = 0;
let dragActive = false;
let quietTicks = 0;
let syncIntervalMs = simSyncIntervalMs(0);
let lastPostMs = -Infinity;
let intervalHandle: ReturnType<typeof setInterval> | null = null;

function post(msg: LayoutWorkerResponse, transfer?: Transferable[]) {
    ctx.postMessage(msg, transfer ?? []);
}

function stopLoop() {
    if (intervalHandle != null) {
        clearInterval(intervalHandle);
        intervalHandle = null;
    }
}

function postPositions(settled: boolean) {
    const positions = new Float32Array(simNodes.length * 2);
    for (let i = 0; i < simNodes.length; i++) {
        positions[2 * i] = simNodes[i].x!;
        positions[2 * i + 1] = simNodes[i].y!;
    }
    post({ type: "positions", runId, positions, settled }, [positions.buffer]);
}

function startLoop() {
    stopLoop();
    quietTicks = 0;
    lastPostMs = -Infinity;
    intervalHandle = setInterval(() => {
        if (!sim) return stopLoop();
        // Tick as many times as fit in the budget — unlike on the main thread
        // there's no UI to starve, so large graphs settle sooner.
        const budgetEndMs = performance.now() + TICK_BUDGET_MS;
        let settled = false;
        do {
            sim.tick();
            let maxV2 = 0;
            for (const node of simNodes) {
                const v2 = (node.vx ?? 0) ** 2 + (node.vy ?? 0) ** 2;
                if (v2 > maxV2) maxV2 = v2;
            }
            quietTicks = maxV2 < SETTLE_MAX_VELOCITY * SETTLE_MAX_VELOCITY ? quietTicks + 1 : 0;
            const quietStop = !dragActive && quietTicks >= SETTLE_QUIET_TICKS;
            settled = sim.alpha() <= sim.alphaMin() || quietStop;
        } while (!settled && performance.now() < budgetEndMs);

        const now = performance.now();
        if (settled || now - lastPostMs >= syncIntervalMs) {
            lastPostMs = now;
            postPositions(settled);
        }
        if (settled) stopLoop();
    }, TICK_INTERVAL_MS);
}

function setSimNodes(nodes: SimNode[]) {
    simNodes = nodes;
    nodeById = new Map(nodes.map(n => [n.id, n]));
    syncIntervalMs = simSyncIntervalMs(nodes.length);
}

ctx.onmessage = (e: MessageEvent<LayoutWorkerRequest>) => {
    const msg = e.data;
    switch (msg.type) {
        case "run": {
            runId = msg.runId;
            dragActive = false;
            setSimNodes(msg.nodes);
            sim = buildForceSimulation(msg.nodes, msg.links, msg.opts);
            startLoop();
            break;
        }
        case "startDrag": {
            runId = msg.runId;
            dragActive = true;
            setSimNodes(msg.nodes);
            sim = buildCollideSimulation(msg.nodes).alphaTarget(DRAG_ALPHA_TARGET).alpha(DRAG_ALPHA_TARGET);
            startLoop();
            break;
        }
        case "endDrag": {
            dragActive = false;
            sim?.alphaTarget(0);
            break;
        }
        case "fixNode": {
            const node = nodeById.get(msg.id);
            if (node) {
                node.x = msg.x;
                node.y = msg.y;
                node.fx = msg.x;
                node.fy = msg.y;
            }
            break;
        }
        case "unfixNode": {
            const node = nodeById.get(msg.id);
            if (node) {
                node.fx = null;
                node.fy = null;
            }
            break;
        }
        case "stop": {
            stopLoop();
            sim = null;
            break;
        }
    }
};

post({ type: "ready" });
