precision mediump float;

uniform float uColorInfluence;
uniform float uTime;
uniform sampler2D myTex;
uniform float uScreenW;
uniform float uScreenH;
uniform int uFlag;

varying vec2 vTextureCoord;
varying vec4 vColor;


#define PI 3.14159;

float circ(vec2 p) {
	float r = length(p);
    r = sqrt(r);

    return abs(1.0 * r * fract(1. - uTime * 10.0));
}

void main() {
    vec4 transparent = vec4(1.0, 1.0, 1.0, 0.0);

    vec2 size = vec2(uScreenW / 25.0, uScreenH / 25.0);
    float posX = vTextureCoord.x - 0.55;
    float posY = vTextureCoord.y - 0.455;
    vec2 uv = vec2(posX, posY) * size;

    float rz = abs(circ(uv));
//    vec3 color = vec3(.0);
//    color = vec3(.1) / rz;

    if (uFlag > 0) {
//        gl_FragColor = vec4(color, 0.5);
        gl_FragColor = vec4(.1, .1, .1, .1) / rz;
    } else {
        gl_FragColor = transparent;
    }
}
