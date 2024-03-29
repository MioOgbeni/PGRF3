#version 150
#define M_PI 3.14159265359

in vec2 inPosition; // input from the vertex buffer

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform float paramFunc;
uniform float moveInTime;

const float delta = 0.001;

// funkce
vec3 funcSaddle(vec2 inPos) {
	// saddle
	float s = inPos.x * 2 - 1;
	float t = inPos.y * 2 -1;
	return vec3(s,t,s*s - t*t);
}

vec3 funcSomething(vec2 inPos) {
	// something
	float s = inPos.y * 8 - 1;
	float t = inPos.x * 8 - 1;

	return vec3(
	(2 + t * cos(s/2) * cos(s)),
	(2 + t * cos(s/2) * sin(s)),
	(t * sin(s/2)));
}

vec3 funcSphere(vec2 inPos) {
	// zemekoule
	float s = M_PI * 0.5 - M_PI * inPos.y;
	float t = 2 * M_PI * inPos.x;
	float r = 2;

	return vec3(
	sin(t) * cos(s) * r,
	cos(t) * cos(s) * r,
	sin(s) * r);
}

vec3 funcDeformedBall(vec2 inPos)
{
	float s = M_PI * 0.5 - M_PI * inPos.y;
	float t = 2 * M_PI * inPos.x;
	float r = 2 + moveInTime *sin(6*s)*sin(5*t);

	return vec3(
	sin(t) * cos(s) * r,
	cos(t) * cos(s) * r,
	sin(s) * r);
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

vec3 funcGlass(vec2 inPos){
	float s= M_PI * 0.5 - M_PI * inPos.x * 2;
	float t= M_PI * 0.5 - M_PI * inPos.y * 2;

	float r = 1 + cos(t);
	float th = s;

	return vec3(
	r*cos(th),
	r*sin(th),
	t);
}


vec3 paramPos(vec2 inPosition){
	vec3 position;

	if (paramFunc == 0) {
		position = funcSphere(inPosition);
	} else if (paramFunc == 1) {
		position = funcDeformedBall(inPosition);
	} else if (paramFunc == 2) {
		position = funcSaddle(inPosition);
	} else if (paramFunc == 3) {
		position = funcSomething(inPosition);
	} else if (paramFunc == 4) {
		position = funcSombrero(inPosition);
	} else if (paramFunc == 5) {
		position = funcGlass(inPosition);
	} else {
		position = vec3(inPosition, 0);
	}
	return position;
}

/* obecny vypocet normaly */
vec3 paramNormal(vec2 inPos){
	vec3 tx = paramPos(inPos + vec2(delta,0)) - paramPos(inPos - vec2(delta,0));
	vec3 ty = paramPos(inPos + vec2(0,delta)) - paramPos(inPos - vec2(0,delta));
	return cross(tx,ty);
}

mat3 paramTangent(vec2 inPos){
	float delta = 0.001;
	vec3 tx = paramPos(inPos + vec2(delta,0)) - paramPos(inPos - vec2(delta,0));
	vec3 ty = paramPos(inPos + vec2(0,delta)) - paramPos(inPos - vec2(0,delta));
	tx= normalize(tx);
	ty = normalize(ty);
	vec3 tz = cross(tx,ty);
	ty = cross(tz,tx);
	return mat3(tx,ty,tz);
}

void main() {
	vec3 position = paramPos(inPosition);
	gl_Position = projection * view * model * vec4(position,1.0);
}