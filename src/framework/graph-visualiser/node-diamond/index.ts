import { Attributes } from "graphology-types";
import { NodeProgram, ProgramInfo } from "sigma/rendering";
import { NodeDisplayData, RenderParams } from "sigma/types";
import { floatColor } from "sigma/utils";

import FRAGMENT_SHADER_SOURCE from "./shader-frag";
import VERTEX_SHADER_SOURCE from "./shader-vert";
import { drawDiamondNodeHover, drawDiamondNodeLabel } from "./utils";

const { UNSIGNED_BYTE, FLOAT } = WebGLRenderingContext;

const UNIFORMS = ["u_sizeRatio", "u_correctionRatio", "u_cameraAngle", "u_matrix"] as const;

const ASPECT = 2.0;
// Bounding box needs to fully contain the diamond. Add a small margin for anti-aliasing.
const MARGIN = 1.05;

export class NodeDiamondProgram<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
> extends NodeProgram<(typeof UNIFORMS)[number], N, E, G> {
    override drawHover = drawDiamondNodeHover;
    override drawLabel = drawDiamondNodeLabel;

    getDefinition() {
        return {
            VERTICES: 6,
            VERTEX_SHADER_SOURCE: VERTEX_SHADER_SOURCE,
            FRAGMENT_SHADER_SOURCE: FRAGMENT_SHADER_SOURCE,
            METHOD: WebGLRenderingContext.TRIANGLES,
            UNIFORMS,
            ATTRIBUTES: [
                { name: "a_position", size: 2, type: FLOAT },
                { name: "a_size", size: 1, type: FLOAT },
                { name: "a_color", size: 4, type: UNSIGNED_BYTE, normalized: true },
                { name: "a_borderColor", size: 4, type: UNSIGNED_BYTE, normalized: true },
                { name: "a_id", size: 4, type: UNSIGNED_BYTE, normalized: true },
            ],
            CONSTANT_ATTRIBUTES: [{ name: "a_offset", size: 2, type: FLOAT }],
            CONSTANT_DATA: [
                [ASPECT * MARGIN, MARGIN],  [-ASPECT * MARGIN, MARGIN],  [ASPECT * MARGIN, -MARGIN],
                [-ASPECT * MARGIN, MARGIN], [ASPECT * MARGIN, -MARGIN], [-ASPECT * MARGIN, -MARGIN],
            ],
        };
    }

    processVisibleItem(nodeIndex: number, startIndex: number, data: NodeDisplayData) {
        const array = this.array;
        const color = floatColor(data.color);
        const borderColor = floatColor((data as any).borderColor || "#00000000");

        array[startIndex++] = data.x;
        array[startIndex++] = data.y;
        array[startIndex++] = data.size;
        array[startIndex++] = color;
        array[startIndex++] = borderColor;
        array[startIndex++] = nodeIndex;
    }

    setUniforms(params: RenderParams, { gl, uniformLocations }: ProgramInfo): void {
        const { u_sizeRatio, u_correctionRatio, u_cameraAngle, u_matrix } = uniformLocations;

        gl.uniform1f(u_sizeRatio, params.sizeRatio);
        gl.uniform1f(u_cameraAngle, params.cameraAngle);
        gl.uniform1f(u_correctionRatio, params.correctionRatio);
        gl.uniformMatrix3fv(u_matrix, false, params.matrix);
    }
}
