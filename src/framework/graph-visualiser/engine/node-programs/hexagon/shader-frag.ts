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

const float BORDER_ABSOLUTE = 0.8;
const vec4 transparent = vec4(0.0, 0.0, 0.0, 0.0);

// Regular hexagon SDF (Inigo Quilez), point-up: corners at top & bottom and
// flat-ish edges left & right. r = circumradius (centre → corner).
// The canonical IQ hexagon is flat-top; we swap x/y so a corner faces up.
float sdHexagon(vec2 p, float r) {
  const vec3 k = vec3(-0.866025404, 0.5, 0.577350269);
  p = abs(p.yx); // swap to make the hexagon point-up
  p -= 2.0 * min(dot(k.xy, p), 0.0) * k.xy;
  p -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
  return length(p) * sign(p.y);
}

void main(void) {
  float u = log2(max(u_sizeRatio, 1.0));
  float borderScale = clamp(1.0 + u * (0.96 + u * (-0.75 + 0.29 * u)), 1.0, 5.0);
  float bw = BORDER_ABSOLUTE * borderScale / v_size;

  // Point-up hexagon. In IQ's SDF r is the apothem (centre to edge midpoint);
  // for a point-up hexagon the top/bottom corners sit at r / cos(30deg) ~= 1.155r.
  // Pick r so those corners land at +/-0.5 (the box's vertical half-extent):
  // r = 0.5 * cos(30deg) ~= 0.433. With a square node this is a regular hexagon.
  float r = 0.4330127;
  float dist = sdHexagon(v_uv, r);

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
