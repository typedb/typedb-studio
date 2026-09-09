import ForceSupervisor from "graphology-layout-force/worker";
import Graph from "graphology";
import MultiGraph from "graphology";
import forceLayout, {ForceLayoutSettings, ForceLayoutParameters} from "graphology-layout-force";
import FA2Layout from 'graphology-layout-forceatlas2/worker';
import forceAtlas2, {
    ForceAtlas2LayoutParameters,
    ForceAtlas2SynchronousLayoutParameters
} from "graphology-layout-forceatlas2";
import FA2LayoutSupervisor from "graphology-layout-forceatlas2/worker";
import noverlap, {NoverlapLayoutParameters} from "graphology-layout-noverlap";
import { forceSimulation, forceLink, forceManyBody, forceCollide, forceCenter, forceX, forceY, SimulationLinkDatum } from "d3-force";
import { runOutsideAngularZone } from "./zone-utils";
import {
    buildCollideSimulation, buildForceSimulation, DRAG_ALPHA_TARGET, LayoutWorkerPositionsMsg, LayoutWorkerRequest,
    LayoutWorkerResponse, SETTLE_MAX_VELOCITY, SETTLE_QUIET_TICKS, SimNode, simSyncIntervalMs,
} from "./d3-force-common";

type D3Node = SimNode;

export class Layouts {

    // Simple, static with no supervisor
    static createForceAtlasStatic(graph: MultiGraph, settings: ForceAtlas2SynchronousLayoutParameters | undefined): LayoutWrapper {
        return new StaticLayoutWrapper(graph, new ForceAtlasStaticWrapper(), settings);
    }

    // This one seems quite versatile. It just needs to be frozen to be able to drag and drop stuff.
    static createForceAtlasSupervisor(graph: MultiGraph, settings: ForceAtlas2LayoutParameters | undefined): LayoutWrapper {
        const factory = () => {
            const resolvedSettings = settings ?? {
                settings: {
                    ...forceAtlas2.inferSettings(Math.max(graph.nodes().length, 1)),
                    adjustSizes: true,
                    gravity: 0,
                },
            };
            return new FA2Layout(graph, resolvedSettings);
        };
        return new LayoutSupervisorWrapper(graph, factory);
    }

    static createForceLayoutStatic(graph: MultiGraph, settings: ForceLayoutSettings | undefined): LayoutWrapper {
        if (settings == undefined) {
            settings = defaultForceLayoutSettings;
        }
        const layout: StaticLayoutInner<ForceLayoutParameters> = {
            assign: (graph: MultiGraph, params: ForceLayoutParameters | undefined) => {
                if (params == undefined) {
                    params = { maxIterations: 1000, settings: defaultForceLayoutSettings };
                }
                forceLayout.assign(graph, params);
            }
        };
        return new StaticLayoutWrapper(graph, layout, { maxIterations: 1000, settings });
    }

    // This one is great at interaction, but it might need parameter tweaking depending on the graph rendered.
    static createForceLayoutSupervisor(graph: MultiGraph, settings: ForceLayoutSettings | undefined): LayoutWrapper {
        if (settings == undefined) {
            settings = defaultForceLayoutSettings;
        }
        const resolvedSettings = settings;
        const factory = () => new ForceSupervisor(graph, {
            isNodeFixed: (_, attr) => attr["highlighted"],
            settings: resolvedSettings,
        });
        return new LayoutSupervisorWrapper(graph, factory);
    }

    // This isn't great. I just used it as an example without a supervisor, though I could just have used the force ones.
    static createLayoutNoOverlap(graph: MultiGraph, settings: NoverlapLayoutParameters | undefined): LayoutWrapper {
        let layout = new NoverlapWrapper();
        return new StaticLayoutWrapper(graph, layout, settings);
    }

    static createD3ForceSupervisor(graph: MultiGraph): LayoutWrapper {
        return new WorkerD3ForceSupervisorWrapper(graph);
    }

    static createD3ForceStatic(graph: MultiGraph): LayoutWrapper {
        const layout: StaticLayoutInner<void> = {
            assign: (graph: MultiGraph) => {
                const nodes: D3Node[] = graph.nodes().map(key => {
                    const attrs = graph.getNodeAttributes(key);
                    return {
                        id: key,
                        x: attrs["x"],
                        y: attrs["y"],
                        radius: Math.max(attrs["width"] ?? attrs["size"] ?? 10, attrs["height"] ?? attrs["size"] ?? 10),
                    };
                });
                const nodeIndex = new Map(nodes.map((n, i) => [n.id, i]));
                const links: SimulationLinkDatum<D3Node>[] = graph.edges().map(edge => ({
                    source: nodeIndex.get(graph.source(edge))!,
                    target: nodeIndex.get(graph.target(edge))!,
                }));

                const vertexCount = nodes.length;
                const edgeCount = links.length;
                const maxRadius = nodes.reduce((max, n) => Math.max(max, n.radius), 0);
                const chargeStrength = ((-500.0 - vertexCount / 3) * (1 + edgeCount / (vertexCount + 1))) * 10;

                const sim = forceSimulation(nodes)
                    .force("charge", forceManyBody().strength(chargeStrength))
                    .force("link", forceLink(links).distance(maxRadius * 3).strength(1))
                    .force("collide", forceCollide<D3Node>().radius(maxRadius * 1.2))
                    .force("center", forceCenter(0, 0))
                    .force("x", forceX(0).strength(0.05))
                    .force("y", forceY(0).strength(0.05))
                    .stop();

                sim.tick(300);

                nodes.forEach(n => {
                    graph.setNodeAttribute(n.id, "x", n.x!);
                    graph.setNodeAttribute(n.id, "y", n.y!);
                });
            }
        };
        return new StaticLayoutWrapper(graph, layout, undefined);
    }
}


/** Node-spacing density presets for the force layout's centering gravity. */
export type LayoutDensity = "spacious" | "default" | "compact";

/** Persistent gravity multiplier per density mode. "default" is 1.5× the base
 *  gravity; "compact" is 4× the base and "spacious" is the default / 3. */
export const DEFAULT_GRAVITY_MULTIPLIER = 1.5;

// Settle/drag/display-sync tuning shared with the layout worker lives in d3-force-common.ts.

/** A physics tick that blocks longer than this yields a proportional number of
 *  frames (capped) before the next tick, so input and paint get through.
 *  Main-thread (fallback) wrapper only — the worker has no UI to starve. */
const SIM_TICK_BUDGET_MS = 8;
const SIM_MAX_YIELD_FRAMES = 5;

/** How long to wait for the layout worker's "ready" before falling back to the
 *  main-thread wrapper (worker script load can fail silently in some webviews). */
const WORKER_READY_TIMEOUT_MS = 3000;
export const DENSITY_GRAVITY: Record<LayoutDensity, number> = {
    spacious: DEFAULT_GRAVITY_MULTIPLIER / 3,
    default: DEFAULT_GRAVITY_MULTIPLIER,
    compact: 4,
};

export interface LayoutStartOptions {
    /** D3-force only: initial alpha (default 1.0). Lower = less perturbation. */
    initialAlpha?: number;
    /** D3-force only: alpha decay rate (default 0.01). Higher = settles faster. */
    alphaDecay?: number;
    /**
     * D3-force only: scales the centering (gravity) force pulling every node
     * toward the origin (default 1). Large values collapse the graph inward.
     */
    gravityMultiplier?: number;
}

export interface LayoutWrapper {
    start(opts?: LayoutStartOptions): void;

    stop(): void;

    // For those that aren't actually animated:
    redraw(): void;

    // Call when you change the graph
    startOrRedraw(): void;

    // Called on each tick of the layout (for animated layouts)
    onTick: (() => void) | null;

    // Whether the layout simulation is currently running
    readonly isRunning: boolean;

    // Pin/unpin a node during drag (no-op for non-animated layouts)
    fixNode(nodeKey: string, x: number, y: number): void;
    unfixNode(nodeKey: string): void;

    /**
     * Drag lifecycle (animated layouts only; optional). `startDrag` reheats the
     * simulation so the dragged node shoves its neighbours aside as it moves;
     * `pinNode` permanently anchors a node at a position (it survives subsequent
     * reheats, so dragging *other* nodes won't push it); `endDrag` lets the
     * simulation cool back down.
     */
    startDrag?(): void;
    endDrag?(): void;
    pinNode?(nodeKey: string, x: number, y: number): void;

    /**
     * Set the node-spacing density (centering-gravity preset) and reheat from
     * current positions so the graph eases into the new spacing. No-op for
     * layouts without an animated, gravity-tunable simulation.
     */
    setDensity(mode: LayoutDensity): void;

    /** The currently-applied node-spacing density. */
    readonly density: LayoutDensity;

    /**
     * Forget which nodes have been settled by a previous simulation. Callers
     * use this when the underlying graph is cleared or every node's position
     * has been deliberately reset (e.g. `reLayout`). No-op for layouts that
     * don't track per-node settling state.
     */
    forgetSettled(): void;

    /** Release resources beyond stop() (e.g. terminate a layout worker). */
    destroy?(): void;
}

type LayoutSupervisor = ForceSupervisor | FA2LayoutSupervisor;

class LayoutSupervisorWrapper implements LayoutWrapper {
    onTick: (() => void) | null = null;
    isRunning = false;
    private layout: LayoutSupervisor | null = null;
    private factory: () => LayoutSupervisor;
    private graph: MultiGraph;
    private stopTimeout: ReturnType<typeof setTimeout> | null = null;
    private runDurationMs: number;

    constructor(graph: MultiGraph, factory: () => LayoutSupervisor, runDurationMs: number = 5000) {
        this.graph = graph;
        this.factory = factory;
        this.runDurationMs = runDurationMs;
    }

    start(_opts?: LayoutStartOptions) {
        this.stop();
        this.layout = this.factory();
        if (this.stopTimeout) clearTimeout(this.stopTimeout);
        this.layout.start();
        this.stopTimeout = setTimeout(() => this.layout?.stop(), this.runDurationMs);
    }

    stop() {
        if (this.stopTimeout) {
            clearTimeout(this.stopTimeout);
            this.stopTimeout = null;
        }
        if (this.layout) {
            this.layout.stop();
            this.layout = null;
        }
    }

    redraw() {
        this.stop();
        this.graph.nodes().forEach(node => {
            this.graph.setNodeAttribute(node, "x", Math.random());
            this.graph.setNodeAttribute(node, "y", Math.random());
        })
        this.start();
    }

    startOrRedraw() {
        this.start();
    }

    fixNode(_nodeKey: string, _x: number, _y: number): void {}
    unfixNode(_nodeKey: string): void {}

    // Force-Atlas2 / generic-force supervisors don't track per-node settling.
    forgetSettled(): void {}

    // No tunable gravity force here — density presets aren't supported.
    readonly density: LayoutDensity = "default";
    setDensity(_mode: LayoutDensity): void {}
}

/**
 * For each node not yet seen by a previous simulation, look at its graph
 * neighbors that *have* settled positions and seed this node at their centroid
 * (with a small jitter to avoid coincident points). New nodes with no settled
 * neighbors keep whatever initial position the graph builder gave them.
 */
function prePositionNewNodes(graph: MultiGraph, settledNodes: Set<string>): void {
    if (settledNodes.size === 0) return;
    const jitter = 10;
    graph.nodes().forEach(key => {
        if (settledNodes.has(key)) return;
        let sumX = 0, sumY = 0, count = 0;
        for (const neighborKey of graph.neighbors(key)) {
            if (!settledNodes.has(neighborKey)) continue;
            const attrs = graph.getNodeAttributes(neighborKey);
            if (attrs["x"] == null || attrs["y"] == null) continue;
            sumX += attrs["x"];
            sumY += attrs["y"];
            count++;
        }
        if (count === 0) return; // no anchor; leave the random position alone
        const cx = sumX / count;
        const cy = sumY / count;
        graph.setNodeAttribute(key, "x", cx + (Math.random() - 0.5) * jitter);
        graph.setNodeAttribute(key, "y", cy + (Math.random() - 0.5) * jitter);
    });
}

/** Snapshot graph nodes as plain sim nodes. Pins are re-applied as fx/fy so
 *  dropped nodes stay anchored; `pinPositions` additionally snaps x/y to the
 *  pin (full runs do, the drag sim keeps current positions). */
function collectSimNodes(graph: MultiGraph, pinned: Map<string, { x: number; y: number }>, pinPositions: boolean): SimNode[] {
    const nodes: SimNode[] = graph.nodes().map(key => {
        const attrs = graph.getNodeAttributes(key);
        return {
            id: key,
            x: attrs["x"],
            y: attrs["y"],
            radius: Math.max(attrs["width"] ?? attrs["size"] ?? 10, attrs["height"] ?? attrs["size"] ?? 10),
        };
    });
    for (const node of nodes) {
        const pin = pinned.get(node.id);
        if (pin) {
            if (pinPositions) { node.x = pin.x; node.y = pin.y; }
            node.fx = pin.x;
            node.fy = pin.y;
        }
    }
    return nodes;
}

function collectSimLinks(graph: MultiGraph, nodes: SimNode[]): { source: number; target: number }[] {
    const nodeIndex = new Map(nodes.map((n, i) => [n.id, i]));
    return graph.edges().map(edge => ({
        source: nodeIndex.get(graph.source(edge))!,
        target: nodeIndex.get(graph.target(edge))!,
    }));
}

class D3ForceSupervisorWrapper implements LayoutWrapper {
    onTick: (() => void) | null = null;
    isRunning = false;
    private graph: MultiGraph;
    private animationFrame: number | null = null;
    private simulation: ReturnType<typeof forceSimulation<D3Node>> | null = null;
    /**
     * Nodes that have been through a complete simulation. New nodes that
     * appear later get pre-positioned near these (via the average of any
     * settled neighbors) so the force simulation doesn't have to drag a
     * cluster of freshly-randomised positions across the canvas.
     */
    private settledNodes: Set<string> = new Set();
    /**
     * Nodes the user has dropped at a chosen position. They get `fx`/`fy`
     * applied on every (re)build so they stay put across reheats — dragging a
     * different node won't shove them. Cleared by `forgetSettled` (Redraw/Reset).
     */
    private pinned: Map<string, { x: number; y: number }> = new Map();
    /**
     * True while a node is being dragged. Suppresses the "everything has gone
     * quiet" early-stop so the warm sim keeps ticking even if the user pauses
     * mid-drag; the run still stops normally once `endDrag` cools alpha.
     */
    private dragActive = false;
    /**
     * Persistent gravity scaling for this simulation, set by the density
     * presets (spacious / default / compact). Stays in effect for every
     * subsequent run until `forgetSettled` resets it to the default (Redraw /
     * Reset changes).
     */
    private gravityMultiplier = DEFAULT_GRAVITY_MULTIPLIER;
    /** Current spacing preset; mirrors `gravityMultiplier` for the UI. */
    density: LayoutDensity = "default";

    constructor(graph: MultiGraph) {
        this.graph = graph;
    }

    forgetSettled(): void {
        this.settledNodes.clear();
        // A fresh layout (Redraw / Reset changes) releases user pins too, so the
        // whole graph re-lays-out freely.
        this.pinned.clear();
        // A fresh layout (Redraw / Reset changes) starts from default gravity.
        this.gravityMultiplier = DEFAULT_GRAVITY_MULTIPLIER;
        this.density = "default";
    }

    private buildSimulation(opts?: LayoutStartOptions): ReturnType<typeof forceSimulation<D3Node>> {
        prePositionNewNodes(this.graph, this.settledNodes);
        const nodes = collectSimNodes(this.graph, this.pinned, true);
        const links = collectSimLinks(this.graph, nodes);
        return buildForceSimulation(nodes, links, {
            initialAlpha: opts?.initialAlpha,
            alphaDecay: opts?.alphaDecay,
            gravityMultiplier: this.gravityMultiplier * (opts?.gravityMultiplier ?? 1),
        });
    }

    start(opts?: LayoutStartOptions) {
        this.stop();
        this.isRunning = true;
        this.simulation = this.buildSimulation(opts);
        this.runLoop(this.simulation);
    }

    /** Drive an (already-built) simulation via rAF until it settles. Shared by
     *  `start` (fresh run) and `reheat` (warm an existing/settled sim). */
    private runLoop(sim: ReturnType<typeof forceSimulation<D3Node>>) {
        let quietTicks = 0;
        let lastSyncMs = -Infinity;
        let yieldFrames = 0;
        const simNodeById = new Map(sim.nodes().map(n => [n.id, n]));
        const syncIntervalMs = simSyncIntervalMs(sim.nodes().length);
        const tick = () => {
            if (yieldFrames > 0) {
                yieldFrames--;
                this.animationFrame = requestAnimationFrame(tick);
                return;
            }
            const tickStartMs = performance.now();
            sim.tick();
            yieldFrames = Math.min(SIM_MAX_YIELD_FRAMES, Math.floor((performance.now() - tickStartMs) / SIM_TICK_BUDGET_MS));
            // Per tick (cheap, no graph writes): find the largest displacement.
            // d3 sets node.vx/vy to the velocity it just applied, so |v| is how
            // far the node moved this tick — used for the early-settle check.
            const nodes = sim.nodes();
            let maxV2 = 0;
            for (const node of nodes) {
                const v2 = (node.vx ?? 0) ** 2 + (node.vy ?? 0) ** 2;
                if (v2 > maxV2) maxV2 = v2;
            }
            quietTicks = maxV2 < SETTLE_MAX_VELOCITY * SETTLE_MAX_VELOCITY ? quietTicks + 1 : 0;
            // Stop once d3's own alpha floor is reached, or (when not dragging)
            // once everything has been visually still for a few ticks — the
            // latter cuts d3's long dead alpha tail. While dragging we keep the
            // sim warm (alphaTarget > 0) and ignore the quiet check, so a paused
            // drag doesn't prematurely stop the simulation.
            const quietStop = !this.dragActive && quietTicks >= SETTLE_QUIET_TICKS;
            const settled = sim.alpha() <= sim.alphaMin() || quietStop;

            // Throttle the expensive part — writing positions into the graph,
            // which triggers Sigma's full re-index + repaint. Physics advances
            // every frame; the display only updates at SIM_DISPLAY_FPS. Always
            // sync the final frame so the graph lands on the true end positions.
            const now = performance.now();
            if (settled || now - lastSyncMs >= syncIntervalMs) {
                lastSyncMs = now;
                // One batched write: a single graphology event (and sigma refresh) instead of two per node.
                this.graph.updateEachNodeAttributes((id, attrs) => {
                    const simNode = simNodeById.get(id);
                    if (simNode) { attrs["x"] = simNode.x!; attrs["y"] = simNode.y!; }
                    return attrs;
                }, { attributes: ["x", "y"] });
                this.onTick?.();
            }

            if (!settled) {
                this.animationFrame = requestAnimationFrame(tick);
            } else {
                this.animationFrame = null;
                this.isRunning = false;
                // Every node that exists at the end of this run is now
                // "settled" — subsequent reheats will pre-position any
                // newly-added nodes near these via their connecting edges.
                this.graph.nodes().forEach(key => this.settledNodes.add(key));
            }
        };
        // Outside Angular's zone, or every frame runs app-wide change detection.
        this.animationFrame = runOutsideAngularZone(() => requestAnimationFrame(tick));
    }

    /**
     * A lightweight simulation used only while dragging: **collision avoidance
     * only**, no charge / link / gravity / centering. Built from the current
     * positions, so a drag merely nudges the nodes the dragged one physically
     * overlaps — overlap resolution is local and damped, so the rest of the
     * graph stays put instead of the whole thing re-settling (no global wobble).
     * Pinned nodes (incl. anything previously dropped) are fixed, so they resist
     * being pushed too.
     */
    private buildDragSimulation(): ReturnType<typeof forceSimulation<D3Node>> {
        return buildCollideSimulation(collectSimNodes(this.graph, this.pinned, false));
    }

    startDrag() {
        this.dragActive = true;
        // Swap in the collide-only drag sim, cancelling any in-flight run first
        // (e.g. dragging mid-initial-layout) so we don't leave two loops going.
        if (this.animationFrame != null) {
            cancelAnimationFrame(this.animationFrame);
            this.animationFrame = null;
        }
        this.simulation = this.buildDragSimulation();
        this.simulation.alphaTarget(DRAG_ALPHA_TARGET).alpha(DRAG_ALPHA_TARGET);
        this.isRunning = true;
        this.runLoop(this.simulation);
    }

    endDrag() {
        this.dragActive = false;
        // Cool down: alpha decays to 0 and the loop settles via the normal path.
        this.simulation?.alphaTarget(0);
    }

    stop() {
        if (this.animationFrame != null) {
            cancelAnimationFrame(this.animationFrame);
            this.animationFrame = null;
        }
        this.simulation = null;
        this.isRunning = false;
    }

    redraw() {
        this.start();
    }

    startOrRedraw() {
        this.start();
    }

    setDensity(mode: LayoutDensity) {
        // Re-selecting the current density is a no-op — no need to reheat.
        if (mode === this.density) return;
        // Set the persistent gravity for the chosen spacing preset, then reheat
        // from current positions so the graph eases into the new spacing.
        this.density = mode;
        this.gravityMultiplier = DENSITY_GRAVITY[mode];
        this.start({ initialAlpha: 0.5, alphaDecay: 0.05 });
    }

    fixNode(nodeKey: string, x: number, y: number): void {
        const d3Node = this.simulation?.nodes().find(n => n.id === nodeKey);
        if (d3Node) {
            d3Node.fx = x;
            d3Node.fy = y;
        }
    }

    unfixNode(nodeKey: string): void {
        // Full release: drop any persistent pin and clear the live fx/fy.
        this.pinned.delete(nodeKey);
        const d3Node = this.simulation?.nodes().find(n => n.id === nodeKey);
        if (d3Node) {
            d3Node.fx = null;
            d3Node.fy = null;
        }
    }

    /** Permanently anchor a node at a position. Survives reheats (re-applied in
     *  buildSimulation), so dragging other nodes later won't push it around. */
    pinNode(nodeKey: string, x: number, y: number): void {
        this.pinned.set(nodeKey, { x, y });
        const d3Node = this.simulation?.nodes().find(n => n.id === nodeKey);
        if (d3Node) {
            d3Node.fx = x;
            d3Node.fy = y;
        }
    }
}

/** The builder rewrites `new URL(...)` to the emitted worker chunk's path only
 *  inside a literal `new Worker(new URL(...))` expression — so capture it via a
 *  stubbed constructor without spawning anything. */
function resolveLayoutWorkerScriptUrl(): URL {
    const g = globalThis as any;
    const nativeWorker = g.Worker;
    let captured: URL | null = null;
    g.Worker = class { constructor(url: URL) { captured = url; } };
    try {
        new Worker(new URL("./d3-force-layout.worker", import.meta.url), { type: "module" });
    } finally {
        g.Worker = nativeWorker;
    }
    return captured!;
}

let layoutWorkerScript$: Promise<{ blobUrl: string; type: WorkerType }> | null = null;

/** WKWebView (Tauri macOS) doesn't route worker script loads through the
 *  custom-protocol handler, so `new Worker(chunkUrl)` hangs silently there. A
 *  main-thread fetch IS intercepted, and a blob: URL then loads in any engine
 *  (same approach as graphology's layout supervisors). Fetched once per session. */
function layoutWorkerScript(): Promise<{ blobUrl: string; type: WorkerType }> {
    if (!layoutWorkerScript$) {
        const scriptUrl = resolveLayoutWorkerScriptUrl();
        layoutWorkerScript$ = fetch(scriptUrl).then(resp => {
            if (!resp.ok) throw new Error(`fetch of ${scriptUrl} failed: HTTP ${resp.status}`);
            return resp.text();
        }).then(code => ({
            blobUrl: URL.createObjectURL(new Blob([code], { type: "text/javascript" })),
            // The chunk is emitted self-contained; load as classic unless it actually contains module syntax.
            type: (/^\s*(?:import|export)\b/m.test(code) ? "module" : "classic") as WorkerType,
        }));
    }
    return layoutWorkerScript$;
}

/** Boot strategy learned this session: "direct" (chunk URL — the only path the
 *  dev server supports), "blob" (fetched + blob-ified, for webviews that don't
 *  route worker script loads through their protocol handler), or "none" (both
 *  failed — go straight to the main-thread fallback, no waiting). */
type LayoutWorkerBootMode = "direct" | "blob" | "none";
let layoutWorkerBootMode: LayoutWorkerBootMode | null = null;

function createDirectLayoutWorker(): Worker {
    return new Worker(new URL("./d3-force-layout.worker", import.meta.url), { type: "module" });
}

/** Runs the d3-force simulation in a web worker so large layouts never block
 *  the main thread; positions arrive as transferable Float32Arrays and are
 *  applied in one batched graph write. Falls back to the in-thread
 *  D3ForceSupervisorWrapper if the worker fails to boot. */
class WorkerD3ForceSupervisorWrapper implements LayoutWrapper {
    onTick: (() => void) | null = null;
    density: LayoutDensity = "default";

    private graph: MultiGraph;
    private worker: Worker | null = null;
    private fallback: D3ForceSupervisorWrapper | null = null;
    private readyTimeout: ReturnType<typeof setTimeout> | null = null;
    private settledNodes: Set<string> = new Set();
    private pinned: Map<string, { x: number; y: number }> = new Map();
    private gravityMultiplier = DEFAULT_GRAVITY_MULTIPLIER;
    private _isRunning = false;
    private runId = 0;
    /** node id → index into the active run's position buffer. */
    private runNodeIndex: Map<string, number> = new Map();
    private destroyed = false;
    /** Commands issued before the (async) worker boot completes. */
    private queuedWhileBooting: LayoutWorkerRequest[] = [];
    /** Latest unapplied positions message — newer messages overwrite older ones
     *  so a slow main thread renders the freshest state instead of a backlog. */
    private pendingPositions: LayoutWorkerPositionsMsg | null = null;
    private applyScheduled = false;
    private nextApplyEarliestMs = 0;
    private runSyncIntervalMs = simSyncIntervalMs(0);

    /** Which boot strategy this instance's live worker used. */
    private bootMode: "direct" | "blob" | null = null;

    constructor(graph: MultiGraph) {
        this.graph = graph;
        // Booted (and handlers bound) outside Angular's zone so position
        // messages don't trigger app-wide change detection.
        runOutsideAngularZone(() => {
            if (layoutWorkerBootMode === "none") this.activateFallback();
            else if (layoutWorkerBootMode === "blob") this.bootBlob();
            else this.bootDirect();
        });
    }

    private bootDirect() {
        try {
            this.adoptWorker(createDirectLayoutWorker(), "direct");
        } catch (err) {
            console.warn("[graph-vis] layout worker (direct) construction failed:", err);
            this.workerFailed("direct");
        }
    }

    private bootBlob() {
        layoutWorkerScript().then(script => {
            if (this.destroyed || this.fallback) return;
            this.adoptWorker(new Worker(script.blobUrl, { type: script.type }), "blob");
        }).catch(err => {
            console.warn("[graph-vis] layout worker (blob) boot failed:", err);
            this.workerFailed("blob");
        });
    }

    private adoptWorker(worker: Worker, mode: "direct" | "blob") {
        this.bootMode = mode;
        worker.onmessage = (e: MessageEvent<LayoutWorkerResponse>) => this.onWorkerMessage(e.data);
        worker.onerror = e => {
            console.warn(`[graph-vis] layout worker (${mode}) error: ${e.message || e.type} (${e.filename || "?"}:${e.lineno ?? "?"})`);
            this.workerFailed(mode);
        };
        this.worker = worker;
        this.readyTimeout = setTimeout(() => {
            console.warn(`[graph-vis] layout worker (${mode}) ready-handshake timed out after ${WORKER_READY_TIMEOUT_MS}ms`);
            this.workerFailed(mode);
        }, WORKER_READY_TIMEOUT_MS);
        // Queued commands are flushed on "ready", so a failed boot can replay
        // them into the next strategy instead of losing them.
    }

    /** Escalate: direct → blob → main-thread fallback. */
    private workerFailed(mode: "direct" | "blob") {
        this.clearReadyTimeout();
        this.worker?.terminate();
        this.worker = null;
        this.bootMode = null;
        if (this.destroyed || this.fallback) return;
        if (mode === "direct") {
            this.bootBlob();
        } else {
            layoutWorkerBootMode = "none";
            this.activateFallback();
        }
    }

    get isRunning(): boolean {
        return this.fallback ? this.fallback.isRunning : this._isRunning;
    }

    private clearReadyTimeout() {
        if (this.readyTimeout != null) {
            clearTimeout(this.readyTimeout);
            this.readyTimeout = null;
        }
    }

    private activateFallback() {
        if (this.fallback || this.destroyed) return;
        console.warn("[graph-vis] layout worker unavailable — simulating on the main thread");
        this.clearReadyTimeout();
        this.worker?.terminate();
        this.worker = null;
        this.queuedWhileBooting = [];
        const fallback = new D3ForceSupervisorWrapper(this.graph);
        fallback.onTick = () => this.onTick?.();
        for (const [key, pos] of this.pinned) fallback.pinNode(key, pos.x, pos.y);
        if (this.density !== "default") fallback.setDensity(this.density);
        this.fallback = fallback;
        if (this._isRunning) fallback.start();
    }

    private onWorkerMessage(msg: LayoutWorkerResponse) {
        if (this.fallback) return;
        if (msg.type === "ready") {
            this.clearReadyTimeout();
            if (layoutWorkerBootMode !== this.bootMode && this.bootMode) {
                layoutWorkerBootMode = this.bootMode;
                console.info(`[graph-vis] layout worker booted (${this.bootMode})`);
            }
            if (this.worker) {
                for (const queued of this.queuedWhileBooting) this.worker.postMessage(queued);
                this.queuedWhileBooting = [];
            }
        } else if (msg.type === "positions" && msg.runId === this.runId) {
            this.pendingPositions = msg;
            this.scheduleApply();
        }
    }

    /** Apply pending positions at a self-limiting pace: after each application
     *  (a full-graph write + sigma repaint), wait at least 1.5× as long as it
     *  took — so rendering can't saturate the main thread however large the
     *  graph or slow the machine. The worker posts freely; stale frames are
     *  simply skipped. */
    private scheduleApply() {
        if (this.applyScheduled || !this.pendingPositions) return;
        this.applyScheduled = true;
        const tryApply = () => {
            if (this.destroyed || this.fallback) { this.applyScheduled = false; return; }
            if (performance.now() < this.nextApplyEarliestMs) {
                requestAnimationFrame(tryApply);
                return;
            }
            this.applyScheduled = false;
            const pending = this.pendingPositions;
            this.pendingPositions = null;
            if (!pending || pending.runId !== this.runId) return;
            const startMs = performance.now();
            this.applyPositions(pending.positions, pending.settled);
            const applyMs = performance.now() - startMs;
            if (applyMs > 100) console.info(`[graph-vis] position sync: ${Math.round(applyMs)}ms`);
            this.nextApplyEarliestMs = performance.now() + Math.max(this.runSyncIntervalMs, applyMs * 1.5);
        };
        requestAnimationFrame(tryApply);
    }

    private applyPositions(positions: Float32Array, settled: boolean) {
        const index = this.runNodeIndex;
        this.graph.updateEachNodeAttributes((id, attrs) => {
            const i = index.get(id);
            if (i != null) {
                attrs["x"] = positions[2 * i];
                attrs["y"] = positions[2 * i + 1];
            }
            return attrs;
        }, { attributes: ["x", "y"] });
        this.onTick?.();
        if (settled) {
            this._isRunning = false;
            this.graph.nodes().forEach(key => this.settledNodes.add(key));
        }
    }

    private post(msg: LayoutWorkerRequest) {
        if (this.worker) this.worker.postMessage(msg);
        else this.queuedWhileBooting.push(msg);
    }

    private beginRun(nodes: SimNode[]) {
        this.runId++;
        this.runNodeIndex = new Map(nodes.map((n, i) => [n.id, i]));
        this.runSyncIntervalMs = simSyncIntervalMs(nodes.length);
        this.pendingPositions = null;
        this._isRunning = true;
    }

    start(opts?: LayoutStartOptions) {
        if (this.fallback) return this.fallback.start(opts);
        prePositionNewNodes(this.graph, this.settledNodes);
        const nodes = collectSimNodes(this.graph, this.pinned, true);
        const links = collectSimLinks(this.graph, nodes);
        this.beginRun(nodes);
        this.post({
            type: "run", runId: this.runId, nodes, links,
            opts: {
                initialAlpha: opts?.initialAlpha,
                alphaDecay: opts?.alphaDecay,
                gravityMultiplier: this.gravityMultiplier * (opts?.gravityMultiplier ?? 1),
            },
        });
    }

    stop() {
        if (this.fallback) return this.fallback.stop();
        this._isRunning = false;
        this.runId++; // discard any in-flight position messages
        this.post({ type: "stop" });
    }

    redraw() {
        this.start();
    }

    startOrRedraw() {
        this.start();
    }

    startDrag() {
        if (this.fallback) return this.fallback.startDrag();
        const nodes = collectSimNodes(this.graph, this.pinned, false);
        this.beginRun(nodes);
        this.post({ type: "startDrag", runId: this.runId, nodes });
    }

    endDrag() {
        if (this.fallback) return this.fallback.endDrag();
        this.post({ type: "endDrag" });
    }

    fixNode(nodeKey: string, x: number, y: number): void {
        if (this.fallback) return this.fallback.fixNode(nodeKey, x, y);
        this.post({ type: "fixNode", id: nodeKey, x, y });
    }

    unfixNode(nodeKey: string): void {
        this.pinned.delete(nodeKey);
        if (this.fallback) return this.fallback.unfixNode(nodeKey);
        this.post({ type: "unfixNode", id: nodeKey });
    }

    pinNode(nodeKey: string, x: number, y: number): void {
        this.pinned.set(nodeKey, { x, y });
        if (this.fallback) return this.fallback.pinNode(nodeKey, x, y);
        this.post({ type: "fixNode", id: nodeKey, x, y });
    }

    setDensity(mode: LayoutDensity) {
        if (mode === this.density) return;
        this.density = mode;
        this.gravityMultiplier = DENSITY_GRAVITY[mode];
        if (this.fallback) return this.fallback.setDensity(mode);
        this.start({ initialAlpha: 0.5, alphaDecay: 0.05 });
    }

    forgetSettled(): void {
        this.settledNodes.clear();
        this.pinned.clear();
        this.gravityMultiplier = DEFAULT_GRAVITY_MULTIPLIER;
        this.density = "default";
        this.fallback?.forgetSettled();
    }

    destroy(): void {
        this.destroyed = true;
        this.clearReadyTimeout();
        this.worker?.terminate();
        this.worker = null;
        this.queuedWhileBooting = [];
        this.fallback?.stop();
        this.fallback = null;
    }
}

interface StaticLayoutInner<LayoutParams> {
    assign(graph: MultiGraph, params: LayoutParams | undefined): void;
}

class StaticLayoutWrapper<LayoutParams> implements LayoutWrapper {
    onTick: (() => void) | null = null;
    isRunning = false;
    private graph: Graph;
    private layout: StaticLayoutInner<LayoutParams>;
    private params: LayoutParams | undefined;

    constructor(graph: MultiGraph, layout: StaticLayoutInner<LayoutParams>, params: LayoutParams | undefined) {
        this.graph = graph;
        this.layout = layout;
        this.params = params;
    }

    start(_opts?: LayoutStartOptions): void {
    }

    stop(): void {
    }

    redraw(): void {
        const spread = Math.max(100, this.graph.order * 10);
        this.graph.nodes().forEach(node => {
            this.graph.setNodeAttribute(node, "x", (Math.random() - 0.5) * spread);
            this.graph.setNodeAttribute(node, "y", (Math.random() - 0.5) * spread);
        });
        this.layout.assign(this.graph, this.params);
    }

    startOrRedraw() {
        this.redraw();
    }

    fixNode(_nodeKey: string, _x: number, _y: number): void {}
    unfixNode(_nodeKey: string): void {}

    // Static layouts don't track per-node settling.
    forgetSettled(): void {}

    // Static layouts have no animated gravity to retune.
    readonly density: LayoutDensity = "default";
    setDensity(_mode: LayoutDensity): void {}
}

class ForceAtlasStaticWrapper implements StaticLayoutInner<ForceAtlas2SynchronousLayoutParameters> {
    static DEFAULT_MAX_ITERATIONS: number = 500;
    assign(graph: MultiGraph, params: ForceAtlas2SynchronousLayoutParameters | undefined): void {
        if (params == undefined) {
            const inferred = forceAtlas2.inferSettings(graph.nodes().length);
            params = {
                iterations: ForceAtlasStaticWrapper.DEFAULT_MAX_ITERATIONS,
                settings: { ...inferred },
            };
        }
        forceAtlas2.assign(graph, params);
    }
}

class NoverlapWrapper implements StaticLayoutInner<NoverlapLayoutParameters> {
    static DEFAULT_MAX_ITERATIONS: number = 50;
    assign(graph: MultiGraph, params: NoverlapLayoutParameters | undefined): void {
        if (params == undefined) {
            params =  { maxIterations: NoverlapWrapper.DEFAULT_MAX_ITERATIONS };
        }
        noverlap.assign(graph, params);
    }
}

export const defaultForceLayoutSettings: ForceLayoutSettings = {
    attraction: 0.0005,
    repulsion: 1.0,
    gravity: 0.00001,
    inertia: 0.6,
    maxMove: 200,
};
