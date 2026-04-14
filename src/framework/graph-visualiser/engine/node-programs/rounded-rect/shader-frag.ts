// language=GLSL
const SHADER_SOURCE = /*glsl*/ `
precision highp float;

varying vec4 v_color;
varying vec4 v_borderColor;
varying vec2 v_uv;
varying float v_aspect;
varying float v_size;

uniform float u_correctionRatio;
uniform float u_sizeRatio;

const float CORNER_RADIUS = 0.25;
const float BORDER_ABSOLUTE = 1.2;
const vec4 transparent = vec4(0.0, 0.0, 0.0, 0.0);

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
  vec2 d = abs(p) - halfSize + r;
  return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main(void) {
  float u = log2(max(u_sizeRatio, 1.0));
  float borderScale = clamp(1.0 + u * (0.96 + u * (-0.75 + 0.29 * u)), 1.0, 5.0);
  float bw = BORDER_ABSOLUTE * borderScale / v_size;
  vec2 halfSize = vec2(v_aspect * 0.5, 0.5);
  float dist = sdRoundedRect(v_uv, halfSize, CORNER_RADIUS);

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
