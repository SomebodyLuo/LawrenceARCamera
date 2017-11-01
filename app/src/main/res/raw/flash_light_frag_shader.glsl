precision mediump float;

uniform float uColorInfluence;
uniform float uTime;
uniform sampler2D myTex;
uniform float uScreenW;
uniform float uScreenH;
uniform int uFlag;

varying vec2 vTextureCoord;
varying vec4 vColor;


void main() {
    if (uFlag > 0) {
        vec2 p = vTextureCoord.xy;
        vec2 q = p - vec2(0.5, 0.5);

        vec3 colour = texture2D(myTex, p.xy).rgb;
        float spot = cos(6.0 * 3.14159 * uTime);
        colour += spot;

        gl_FragColor = vec4(colour, 0.6);
    }
}
