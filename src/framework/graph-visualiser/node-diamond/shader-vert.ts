// language=GLSL
const SHADER_SOURCE = /*glsl*/ `
attribute vec4 a_id;
attribute vec4 a_color;
attribute vec4 a_borderColor;
attribute vec2 a_position;
attribute float a_size;
attribute float a_aspect;
attribute vec2 a_offset;

uniform mat3 u_matrix;
uniform float u_sizeRatio;
uniform float u_cameraAngle;
uniform float u_correctionRatio;

varying vec4 v_color;
varying vec4 v_borderColor;
varying vec2 v_uv;
varying float v_aspect;
varying float v_size;

const float bias = 255.0 / 254.0;

void main() {
  float size = a_size * u_correctionRatio / u_sizeRatio * 4.0;
  vec2 scaledOffset = vec2(a_offset.x * a_aspect, a_offset.y);
  float ca = cos(u_cameraAngle);
  float sa = sin(u_cameraAngle);
  vec2 rotatedOffset = vec2(
    scaledOffset.x * ca - scaledOffset.y * sa,
    scaledOffset.x * sa + scaledOffset.y * ca
  );
  vec2 diffVector = size * rotatedOffset;
  vec2 position = a_position + diffVector;
  gl_Position = vec4(
    (u_matrix * vec3(position, 1)).xy,
    0,
    1
  );

  v_uv = scaledOffset;
  v_aspect = a_aspect;
  v_size = a_size;

  #ifdef PICKING_MODE
  v_color = a_id;
  v_borderColor = a_id;
  #else
  v_color = a_color;
  v_borderColor = a_borderColor;
  #endif

  v_color.a *= bias;
  v_borderColor.a *= bias;
}
`;

export default SHADER_SOURCE;
