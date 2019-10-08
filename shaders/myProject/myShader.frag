#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 normal_IO;
out vec4 outColor; // output from the fragment shader
void main() {
    vec3 light = vec3(10);
    float cosAlpha = dot(normal_IO, normalize(light));
	outColor = vec4(vec3(cosAlpha), 1.0);
	//outColor = vec4(vertColor, 1.0);
}