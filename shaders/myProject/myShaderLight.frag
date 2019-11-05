#version 150

out vec4 outColor; // output from the fragment shader

void main() {
    outColor = vec4(gl_FragCoord.zzz, 1);
}