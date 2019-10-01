#version 150
in vec2 inPosition; // input from the vertex buffer
in vec3 inColor; // input from the vertex buffer
out vec3 vertColor; // output from this shader to the next pipeline stage
uniform float time; // variable constant for all vertices in a single draw
uniform mat4 MVP;

void main() {
	vec2 position = inPosition;
	position.xy += -0.5;
	float z = -((position.x *position.x*2)+(position.y*position.y*2));
	gl_Position = MVP * vec4(position.x, z, position.y, 1.0);
	vertColor = inColor;
}