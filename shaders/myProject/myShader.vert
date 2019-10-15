#version 150
#define M_PI 3.14159265359

in vec2 inPosition; // input from the vertex buffer

out vec2 outPosition;
out vec3 vertColor; // output from this shader to the next pipeline stage
out vec3 normal_IO;
out vec3 lightVec;
out vec3 eyeVec;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 rotateX;
uniform vec3 lightPos;
uniform vec3 eyePos;
uniform float paramFunc;

const float delta = 0.001;

// funkce
vec3 funcSaddle(vec2 inPos) {
	float s = inPos.x * 2 - 1;
	float t = inPos.y * 2 -1;
	return vec3(s,t,s*s - t*t);
}

vec3 funcSombrero(vec2 inPos) {
	// sombrero
		float s = M_PI * 0.5 - M_PI * inPos.x *2;
		float t = 2 * M_PI * inPos.y;

		return vec3(
				t*cos(s),
				t*sin(s),
				2*sin(t))/2;
}

vec3 funcSphere(vec2 inPos) {
	//zemekoule
	float s = M_PI * 0.5 - M_PI * inPos.y;
	float t = 2* M_PI * inPos.x;
	float r = 2;

	return vec3(
			cos(t) * cos(s) * r,
			sin(t) * cos(s) * r,
			sin(s) * r);
}

vec3 paramPos(vec2 inPosition){
	vec3 position;

	if (paramFunc == 0) {
		position = funcSaddle(inPosition);
	} else if (paramFunc == 1) {
		position = funcSombrero(inPosition);
	} else if (paramFunc == 2) {
		position = funcSphere(inPosition);
	}
	return position;
}

/* obecny vypocet normaly */
vec3 paramNormal(vec2 inPos){
	vec3 tx = paramPos(inPos + vec2(delta,0)) - paramPos(inPos - vec2(delta,0));
	vec3 ty = paramPos(inPos + vec2(0,delta)) - paramPos(inPos - vec2(0,delta));
	return cross(tx,ty);
}

void main() {
    outPosition = inPosition;
    vec3 position = paramPos(inPosition);
	vec3 normal = normalize(paramNormal(inPosition));
	gl_Position = projection * view * rotateX * vec4(position,1.0);

	vertColor = vec3(normal);
	normal_IO = normal;
	lightVec = normalize(lightPos - (rotateX * vec4(position,1.0)).xyz);
}