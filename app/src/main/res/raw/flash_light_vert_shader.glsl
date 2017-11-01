precision mediump float;

uniform mat4 uMVPMatrix;
uniform float uTime;

attribute vec4 aPosition;
attribute vec2 aTextureCoord;

varying vec4 vColor;
varying vec2 vTextureCoord;

void main() {
	vTextureCoord = aTextureCoord;
	gl_Position = uMVPMatrix * aPosition;
}