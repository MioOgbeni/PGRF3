#version 150
in vec2 inPosition; // input from the vertex buffer
in vec3 inColor; // input from the vertex buffer
out vec3 vertColor; // output from this shader to the next pipeline stage
out vec3 normal_IO;
uniform mat4 projection;
uniform mat4 view;
uniform mat4 rotateX;

float getFunctionValue(vec2 position){
    return -((position.x *position.x*2)+(position.y*position.y*2));
}

vec3 getNormal(vec2 position){
    float delta = 0.01;
    vec3 u = vec3(position + vec2(delta, 0), getFunctionValue(position + vec2(delta, 0)))
           - vec3(position - vec2(delta, 0), getFunctionValue(position - vec2(delta, 0)));
    vec3 v = vec3(position + vec2(0, delta), getFunctionValue(position + vec2(0, delta)))
           - vec3(position - vec2(0, delta), getFunctionValue(position - vec2(0, delta)));
    return cross(u,v);
}

void main() {
	vec2 position = inPosition;
	position.xy -= 0.5;
	float z = getFunctionValue(position.xy);
	vec3 normal = normalize(getNormal(position.xy));
	gl_Position = projection * view * rotateX * vec4(position.x, z, position.y, 1.0);
	vertColor = vec3(normal);
	normal_IO = normal;
}