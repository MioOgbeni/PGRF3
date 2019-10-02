#version 150
in vec2 inPosition; // input from the vertex buffer
in vec3 inColor; // input from the vertex buffer
out vec3 vertColor; // output from this shader to the next pipeline stage
uniform mat4 projection;
uniform mat4 view;
uniform mat4 rotateX;

void main() {
	vec2 position = inPosition;
	position.xy += -0.5;
	float z = -((position.x *position.x*2)+(position.y*position.y*2));
	gl_Position = projection * view * rotateX * vec4(position.x, z, position.y, 1.0);
	vertColor = inColor;
}