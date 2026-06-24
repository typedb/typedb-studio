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
import { forceSimulation, forceLink, forceManyBody, forceCollide, forceCenter, forceX, forceY, SimulationNodeDatum, SimulationLinkDatum } from "d3-force";

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
        return new D3ForceSupervisorWrapper(graph);
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

/**
 * Early-stop thresholds for the force sim. d3 keeps ticking until alpha decays
 * to alphaMin, but alpha decays exponentially — so the run has a long tail
 * (several seconds) where alpha is still above alphaMin yet every node is
 * effectively stationary. Each of those tail ticks re-renders the whole graph
 * (every label repainted), which is pure waste. Instead we stop once the
 * fastest-moving node has been below SETTLE_MAX_VELOCITY (graph units / tick,
 * i.e. its per-tick displacement) for SETTLE_QUIET_TICKS consecutive ticks —
 * "nobody is visibly moving any more". The consecutive-tick guard avoids
 * stopping during a brief lull early in the run when forces momentarily cancel.
 */
const SETTLE_MAX_VELOCITY = 0.3;
const SETTLE_QUIET_TICKS = 5;

/**
 * Display refresh cap for a running sim. Writing node x/y into the graph is what
 * makes Sigma re-index and repaint the *entire* graph (a pan, by contrast, only
 * re-renders with the existing index) — so doing it every animation frame makes
 * the sim try to fully repaint 60×/s, which a large graph can't sustain. The
 * physics still ticks every frame; we just push positions to the display at most
 * this often, so the sim renders at a steady, affordable rate instead of
 * saturating the main thread. Lower it further if very large graphs still jank.
 */
const SIM_DISPLAY_FPS = 30;
const SIM_MIN_SYNC_INTERVAL_MS = 1000 / SIM_DISPLAY_FPS;

/**
 * Alpha target the (collide-only) drag simulation is held at while a node is
 * being dragged — keeps it warm/ticking so the fixed, cursor-following node
 * resolves overlaps with the nodes it bumps into. On drop we set the target
 * back to 0 so it cools and settles.
 */
const DRAG_ALPHA_TARGET = 0.3;
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

interface D3Node extends SimulationNodeDatum {
    id: string;
    radius: number;
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

    /**
     * For each node not yet seen by a previous simulation, look at its
     * graph neighbors that *have* settled positions and seed this node at
     * their centroid (with a small jitter to avoid coincident points). New
     * nodes with no settled neighbors keep whatever initial position the
     * graph builder gave them.
     */
    private prePositionNewNodes(): void {
        if (this.settledNodes.size === 0) return;
        const jitter = 10;
        this.graph.nodes().forEach(key => {
            if (this.settledNodes.has(key)) return;
            let sumX = 0, sumY = 0, count = 0;
            for (const neighborKey of this.graph.neighbors(key)) {
                if (!this.settledNodes.has(neighborKey)) continue;
                const attrs = this.graph.getNodeAttributes(neighborKey);
                if (attrs["x"] == null || attrs["y"] == null) continue;
                sumX += attrs["x"];
                sumY += attrs["y"];
                count++;
            }
            if (count === 0) return; // no anchor; leave the random position alone
            const cx = sumX / count;
            const cy = sumY / count;
            this.graph.setNodeAttribute(key, "x", cx + (Math.random() - 0.5) * jitter);
            this.graph.setNodeAttribute(key, "y", cy + (Math.random() - 0.5) * jitter);
        });
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
        this.prePositionNewNodes();
        const nodes: D3Node[] = this.graph.nodes().map(key => {
            const attrs = this.graph.getNodeAttributes(key);
            return {
                id: key,
                x: attrs["x"],
                y: attrs["y"],
                radius: Math.max(attrs["width"] ?? attrs["size"] ?? 10, attrs["height"] ?? attrs["size"] ?? 10),
            };
        });
        // Re-apply user pins so dropped nodes stay anchored across rebuilds.
        for (const node of nodes) {
            const pin = this.pinned.get(node.id);
            if (pin) { node.x = pin.x; node.y = pin.y; node.fx = pin.x; node.fy = pin.y; }
        }
        const nodeIndex = new Map(nodes.map((n, i) => [n.id, i]));
        const links: SimulationLinkDatum<D3Node>[] = this.graph.edges().map(edge => ({
            source: nodeIndex.get(this.graph.source(edge))!,
            target: nodeIndex.get(this.graph.target(edge))!,
        }));

        const vertexCount = nodes.length;
        const edgeCount = links.length;
        const maxRadius = nodes.reduce((max, n) => Math.max(max, n.radius), 0);
        const baseCharge = -1500 * (1 + Math.log(1 + edgeCount / (vertexCount + 1)));

        // Compute per-node degree so hubs (cluster centers) repel harder
        const degree: number[] = new Array(nodes.length).fill(0);
        links.forEach(l => {
            degree[l.source as number]++;
            degree[l.target as number]++;
        });

        const n = nodes.length;
        const chargeStrength = -Math.max(50, Math.min(300, n * 2));

        // Detect connected components (islands)
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

        const gravityStrength = (componentCount > 1 ? 0.06 : 0.02) * this.gravityMultiplier * (opts?.gravityMultiplier ?? 1);

        return forceSimulation(nodes)
            .force("charge", forceManyBody()
                .strength(baseCharge))
                // .distanceMax(maxRadius * 20))
            .force("link", forceLink(links).distance(maxRadius).strength(1))
            .force("collide", forceCollide<D3Node>().radius(maxRadius))
            .force("center", forceCenter(0, 0))
            .force("x", forceX(0).strength(gravityStrength))
            .force("y", forceY(0).strength(gravityStrength))
            .alpha(opts?.initialAlpha ?? 1.0)
            .alphaDecay(opts?.alphaDecay ?? 0.01)
            .stop();
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
        const tick = () => {
            sim.tick();
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
            if (settled || now - lastSyncMs >= SIM_MIN_SYNC_INTERVAL_MS) {
                lastSyncMs = now;
                for (const node of nodes) {
                    this.graph.setNodeAttribute(node.id, "x", node.x!);
                    this.graph.setNodeAttribute(node.id, "y", node.y!);
                }
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
        this.animationFrame = requestAnimationFrame(tick);
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
        const nodes: D3Node[] = this.graph.nodes().map(key => {
            const attrs = this.graph.getNodeAttributes(key);
            return {
                id: key,
                x: attrs["x"],
                y: attrs["y"],
                radius: Math.max(attrs["width"] ?? attrs["size"] ?? 10, attrs["height"] ?? attrs["size"] ?? 10),
            };
        });
        for (const node of nodes) {
            const pin = this.pinned.get(node.id);
            if (pin) { node.fx = pin.x; node.fy = pin.y; }
        }
        return forceSimulation(nodes)
            .force("collide", forceCollide<D3Node>().radius(d => d.radius))
            .alphaDecay(0.05)
            .stop();
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
