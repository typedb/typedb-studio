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


export interface LayoutWrapper {
    start(): void;

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

    start() {
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

    constructor(graph: MultiGraph) {
        this.graph = graph;
    }

    private buildSimulation(): ReturnType<typeof forceSimulation<D3Node>> {
        const nodes: D3Node[] = this.graph.nodes().map(key => {
            const attrs = this.graph.getNodeAttributes(key);
            return {
                id: key,
                x: attrs["x"],
                y: attrs["y"],
                radius: Math.max(attrs["width"] ?? attrs["size"] ?? 10, attrs["height"] ?? attrs["size"] ?? 10),
            };
        });
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

        const gravityStrength = componentCount > 1 ? 0.06 : 0.02;

        return forceSimulation(nodes)
            .force("charge", forceManyBody()
                .strength(baseCharge))
                // .distanceMax(maxRadius * 20))
            .force("link", forceLink(links).distance(maxRadius).strength(1))
            .force("collide", forceCollide<D3Node>().radius(maxRadius))
            .force("center", forceCenter(0, 0))
            .force("x", forceX(0).strength(gravityStrength))
            .force("y", forceY(0).strength(gravityStrength))
            .alphaDecay(0.01)
            .stop();
    }

    start() {
        this.stop();
        this.isRunning = true;
        this.simulation = this.buildSimulation();
        const sim = this.simulation;
        const tick = () => {
            sim.tick();
            sim.nodes().forEach(node => {
                this.graph.setNodeAttribute(node.id, "x", node.x!);
                this.graph.setNodeAttribute(node.id, "y", node.y!);
            });
            this.onTick?.();
            if (sim.alpha() > sim.alphaMin()) {
                this.animationFrame = requestAnimationFrame(tick);
            } else {
                this.animationFrame = null;
                this.isRunning = false;
            }
        };
        this.animationFrame = requestAnimationFrame(tick);
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

    fixNode(nodeKey: string, x: number, y: number): void {
        const d3Node = this.simulation?.nodes().find(n => n.id === nodeKey);
        if (d3Node) {
            d3Node.fx = x;
            d3Node.fy = y;
        }
    }

    unfixNode(nodeKey: string): void {
        const d3Node = this.simulation?.nodes().find(n => n.id === nodeKey);
        if (d3Node) {
            d3Node.fx = null;
            d3Node.fy = null;
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

    start(): void {
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
