#version 150
#define M_PI 3.14159265359

in vec2 inPosition; // input from the vertex buffer

out vec3 vertPosition;
out vec3 vertColor; // output from this shader to the next pipeline stage
out vec3 vertColorNormal;
out vec3 vertNormal;
out vec4 coordLight;

out vec3 fragPos;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform mat4 viewLight;

uniform float paramFunc;
uniform float moveInTime;

uniform float lightType;

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
	float s = inPos.x * 8 - 1;
	float t = inPos.y * 8 - 1;

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
			cos(t) * cos(s) * r,
			sin(t) * cos(s) * r,
			sin(s) * r);
}

vec3 funcDeformedBall(vec2 vec)
{
	float s = vec.x * M_PI;
	float t = vec.y * M_PI * 2;

	float rho = 1+ moveInTime *sin(6*s)*sin(5*t);
	float phi = t;
	float theta = s;

	float x = rho * sin(phi) * cos(theta);
	float y = rho * sin(phi) * sin(theta);
	float z = rho * cos(phi);

	return vec3(x, y, z);
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

void main() {
	vertPosition = vec3(inPosition, 1.0);
	vec3 position = paramPos(inPosition);
	vec3 normal = transpose(inverse(mat3(model))) * paramNormal(inPosition);

	vertColor =  (model * vec4(position,1.0)).xyz;
	vertColorNormal = paramNormal(inPosition);
	vertNormal = normal;
	fragPos = vec3(model * vec4(position, 1.0));

	coordLight = projection * viewLight * model * vec4(position, 1.0);
	gl_Position = projection * view * model * vec4(position, 1.0);
}