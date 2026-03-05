import ForceSupervisor from "graphology-layout-force/worker";
import * as studioDefaultSettings from "./defaults";
import Graph from "graphology";
import MultiGraph from "graphology";
import {ForceLayoutSettings} from "graphology-layout-force";
import FA2Layout from 'graphology-layout-forceatlas2/worker';
import forceAtlas2, {
    ForceAtlas2LayoutParameters,
    ForceAtlas2SynchronousLayoutParameters
} from "graphology-layout-forceatlas2";
import FA2LayoutSupervisor from "graphology-layout-forceatlas2/worker";
import noverlap, {NoverlapLayoutParameters} from "graphology-layout-noverlap";

export class Layouts {

    // Simple, static with no supervisor
    static createForceAtlasStatic(graph: MultiGraph, settings: ForceAtlas2SynchronousLayoutParameters | undefined): LayoutWrapper {
        return new StaticLayoutWrapper(graph, new ForceAtlasStaticWrapper(), settings);
    }

    // This one seems quite versatile. It just needs to be frozen to be able to drag and drop stuff.
    static createForceAtlasSupervisor(graph: MultiGraph, settings: ForceAtlas2LayoutParameters | undefined): LayoutWrapper {
        let layout = new FA2Layout(graph, settings);
        return new LayoutSupervisorWrapper(graph, layout);
    }

    // This one is great at interaction, but it might need parameter tweaking depending on the graph rendered.
    static createForceLayoutSupervisor(graph: MultiGraph, settings: ForceLayoutSettings | undefined): LayoutWrapper {
        if (settings == undefined) {
            settings = studioDefaultSettings.defaultForceSupervisorSettings;
        }
        let layout = new ForceSupervisor(graph, {
            isNodeFixed: (_, attr) => attr["highlighted"],
            settings: settings,
        });
        return new LayoutSupervisorWrapper(graph, layout);
    }

    // This isn't great. I just used it as an example without a supervisor, though I could just have used the force ones.
    static createLayoutNoOverlap(graph: MultiGraph, settings: NoverlapLayoutParameters | undefined): LayoutWrapper {
        let layout = new NoverlapWrapper();
        return new StaticLayoutWrapper(graph, layout, settings);
    }

    // We can add our own layouts too. We just have to set the nodeAttributes "x" and "y".
}


export interface LayoutWrapper {
    start(): void;

    stop(): void;

    // For those that aren't actually animated:
    redraw(): void;

    // Call when you change the graph
    startOrRedraw(): void;

}

class LayoutSupervisorWrapper implements LayoutWrapper {
    private layout: ForceSupervisor | FA2LayoutSupervisor;
    private graph: MultiGraph;

    constructor(graph: MultiGraph, layout: ForceSupervisor | FA2LayoutSupervisor) {
        this.graph = graph;
        this.layout = layout;
    }

    start() {
        this.layout.start();
    }

    stop() {
        this.layout.stop();
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
}

interface StaticLayoutInner<LayoutParams> {
    assign(graph: MultiGraph, params: LayoutParams | undefined): void;
}

class StaticLayoutWrapper<LayoutParams> implements LayoutWrapper {
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
        this.graph.nodes().forEach(node => {
            this.graph.setNodeAttribute(node, "x", Math.random());
            this.graph.setNodeAttribute(node, "y", Math.random());
        });
        this.layout.assign(this.graph, this.params);
    }

    startOrRedraw() {
        this.redraw();
    }
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

        // FA2 treats nodes as points. Scale positions proportionally to visual
        // node sizes so there's enough room for noverlap to resolve overlaps
        // without disrupting the topology.
        let maxVisualSize = 0;
        graph.forEachNode((_, attrs) => {
            maxVisualSize = Math.max(maxVisualSize, Math.max(attrs["width"] ?? attrs["size"], attrs["height"] ?? attrs["size"]) * 2.0);
        });
        const posScale = Math.max(1, maxVisualSize / 15);
        graph.forEachNode((node) => {
            graph.setNodeAttribute(node, "x", graph.getNodeAttribute(node, "x") * posScale);
            graph.setNodeAttribute(node, "y", graph.getNodeAttribute(node, "y") * posScale);
        });

        // Inflate sizes to visual half-widths for noverlap, then restore.
        const savedSizes = new Map<string, number>();
        graph.forEachNode((node, attrs) => {
            savedSizes.set(node, attrs["size"]);
            const visualSize = Math.max(attrs["width"] ?? attrs["size"], attrs["height"] ?? attrs["size"]) * 2.0;
            graph.setNodeAttribute(node, "size", visualSize);
        });
        noverlap.assign(graph, { maxIterations: 300, settings: { margin: 5 } });
        savedSizes.forEach((size, node) => {
            graph.setNodeAttribute(node, "size", size);
        });
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

