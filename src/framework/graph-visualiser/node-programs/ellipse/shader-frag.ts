// language=GLSL
const SHADER_SOURCE = /*glsl*/ `
precision highp float;

varying vec4 v_color;
varying vec4 v_borderColor;
varying vec2 v_uv;
varying float v_aspect;
varying float v_size;

uniform float u_correctionRatio;

const float BORDER_ABSOLUTE = 1.2;
const vec4 transparent = vec4(0.0, 0.0, 0.0, 0.0);

void main(void) {
  float bw = BORDER_ABSOLUTE / v_size;
  vec2 halfSize = vec2(v_aspect * 0.5, 0.5);
  vec2 scaled = v_uv / halfSize;
  float len = length(scaled);
  float rawDist = len - 1.0;
  // Gradient correction: normalize by |∇f| so border width is uniform around the ellipse
  vec2 grad = (scaled / halfSize) / max(len, 0.001);
  float dist = rawDist / length(grad);

  float aaWidth = u_correctionRatio * 2.0;

  #ifdef PICKING_MODE
  if (dist > aaWidth)
    gl_FragColor = transparent;
  else
    gl_FragColor = v_color;
  #else
  if (dist > aaWidth) {
    gl_FragColor = transparent;
  } else if (dist > 0.0) {
    float t = dist / aaWidth;
    gl_FragColor = mix(v_borderColor, transparent, t);
  } else if (dist > -bw) {
    float innerT = smoothstep(-bw, -bw + aaWidth, dist);
    gl_FragColor = mix(v_color, v_borderColor, innerT);
  } else {
    gl_FragColor = v_color;
  }
  #endif
}
`;

export default SHADER_SOURCE;
