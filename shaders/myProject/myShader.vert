#version 150
#define M_PI 3.14159265359

in vec2 inPosition; // input from the vertex buffer

out vec2 outPosition;
out vec3 vertColor; // output from this shader to the next pipeline stage
out vec3 normal_IO;
out vec3 lightVec;
out vec3 eyeVec;
out mat3 tbn;
out vec2 texCoord;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 rotateX;
uniform vec3 lightPos;
uniform vec3 eyePos;
uniform float paramFunc;

uniform float lightType;
uniform float renderTexture;
uniform float mappingType;

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
		position = funcSaddle(inPosition);
	} else if (paramFunc == 1) {
		position = funcSombrero(inPosition);
	} else if (paramFunc == 2) {
		position = funcSphere(inPosition);
	} else if (paramFunc == 3) {
     	position = funcGlass(inPosition);
    } else if (paramFunc == 4) {
     	position = funcSomething(inPosition);
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
    outPosition = inPosition;
    vec3 position = paramPos(inPosition);
	vec3 normal = normalize(paramNormal(inPosition));
	gl_Position = projection * view * rotateX * vec4(position,1.0);

	lightVec = normalize(lightPos - position);
	eyeVec = normalize(eyePos - position);
	normal_IO = normal;

		// paralax mapping
    	if(mappingType==1){
    		/* parallax mapping */
    		tbn = paramTangent(inPosition);
    	}

    	// osvetleni per vertex
    	if (lightType == 0) {

    		float diff = max(0,dot(normal, lightVec));
    		vec3 halfVec = normalize(eyeVec + lightVec);
    		float spec = dot(normal, halfVec);
    		spec = max(0,spec);
    		spec = pow(spec, 10);
    		float ambient = 0.1;

    		if (renderTexture == 1) {
    			vertColor=vec3(1,1,1) * (min(ambient + diff,1)) + vec3(1,1,1) * spec;
    		} else {
    			vertColor=vec3(inPosition,0) * (min(ambient + diff,1)) + vec3(1,1,1) * spec;
    		}

    	}

    	//textury

    	if (renderTexture == 1) {
    		int aux = int(dot(abs(normal) * vec3(0, 1, 2), vec3(1, 1, 1)));
    		texCoord = vec2(inPosition[(aux + 1) % 3], inPosition[(aux + 2) % 3]);
    	}
}