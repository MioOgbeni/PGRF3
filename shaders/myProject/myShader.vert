#version 150
#define M_PI 3.14159265359

in vec2 inPosition; // input position from the vertex buffer

out vec3 vertPosition;
out vec3 vertColor; // output from this shader to the next pipeline stage
out vec3 vertNormal;
out vec4 coordLight;

out vec3 lightDir;
out vec3 viewDir;
out vec3 halfwayDir;
out float lightDistance;

out vec3 outSpotDir;

out vec3 verLighting;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform mat4 viewLight;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 spotDir;

uniform float paramFunc;
uniform float moveInTime;

uniform float lightType;

const float delta = 0.001;

uniform float surfaceType;
uniform vec3 objectColor;
uniform bool ascii;
uniform bool moon;
uniform bool spotlight;
uniform bool coordsInTexture;
uniform bool lightBulb;

uniform sampler2D mainTex;
uniform sampler2D mainHighTex;
uniform sampler2D moonTex;
uniform sampler2D moonHighTex;
uniform sampler2D shadowMap;

const float constantAttenuation = 1.0;
const float linearAttenuation = 0.14;
const float quadraticAttenuation = 0.07;

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
	// globe
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
	// deformed ball
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
	// glass
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

// normal calculation
vec3 paramNormal(vec2 inPos){
	vec3 tx = paramPos(inPos + vec2(delta,0)) - paramPos(inPos - vec2(delta,0));
	vec3 ty = paramPos(inPos + vec2(0,delta)) - paramPos(inPos - vec2(0,delta));
	return cross(tx,ty);
}

float ShadowCalculation(vec4 coordLight)
{
	//dehomog
    vec3 projCoords = coordLight.xyz / coordLight.w;
	//to range [0 1]
    projCoords = projCoords * 0.5 + 0.5;
	//take a depth of fragment
    float currentDepth = projCoords.z;
	//set bias
    float bias = 0.005;

    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
	float shadow = 0.0;

	//go through surrounding pixels
    for(int x = -3; x <= 3; ++x)
    {
        for(int y = -3; y <= 3; ++y)
        {
			//calculate shadow for surrounding pixels
            float xySubFragDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
			if(currentDepth - bias > xySubFragDepth){
				shadow += 1.0;
			}
        }
    }
	//smooth shadow
    shadow /= 49.0;

    return shadow;
}

void main() {
	vertPosition = vec3(inPosition, 1.0);
	vec3 position = paramPos(inPosition);
	vec3 normal = transpose(inverse(mat3(view * model))) * paramNormal(inPosition);

	vertColor =  (model * vec4(position,1.0)).xyz;
	vertNormal = normalize(normal);

	vec3 fragPos = vec3(view * model * vec4(position, 1.0));
	lightDir = normalize((mat3(view) * lightPos) - fragPos);
	viewDir = normalize((mat3(view) * viewPos) - fragPos);
	halfwayDir = normalize(lightDir + viewDir);
    lightDistance = length(lightDir);

    outSpotDir = normalize(lightPos - vec3(model * vec4(position, 1.0)));

	coordLight = projection * viewLight * model * vec4(position, 1.0);
	gl_Position = projection * view * model * vec4(position, 1.0);

    if(lightType == 1){
        vec3 color;

        if (surfaceType == 1){
            // texture
			if(!moon){
				if(coordsInTexture){
					color = vec3(vertPosition.xy, 1.0);
				}else{
					normal = texture2D(mainHighTex, vertPosition.xy).xyz;
					normal *= 2;
					normal -= 1;
					color =  texture(mainTex, vertPosition.xy).rgb;
				}
			}else{
				if(coordsInTexture){
					color = vec3(vertPosition.xy, 1.0);
				}else{
					normal = texture2D(moonHighTex, vertPosition.xy).xyz;
					normal *= 2;
					normal -= 1;
					color =  texture(moonTex, vertPosition.xy).rgb;
				}
			}
        } else if (surfaceType == 2){
            // normal color
            float cosAlpha = dot(normalize(vertNormal), normalize(lightDir));
            color =  vec3(cosAlpha);
        } else if (surfaceType == 3){
            // xyz color
            color = vertColor;
        } else if (surfaceType == 4){
            // depth color
            color = (projection * view * model * vec4(position, 1.0)).zzz;
        }else{
            //  solid color from cpu
            color = objectColor/255;
        }

        //light parameters declaration
        float ambientStrength = 0.2;
        float specularStrength = 0.5;
        vec3 lightColor = vec3(1.0);

        //ambient
        vec3 ambient = ambientStrength * lightColor;

        //difuse
        float diff = max(dot(normalize(vertNormal), normalize(lightDir)), 0.0);
        vec3 diffuse = diff * lightColor;

        //specular
        //phong
        	//vec3 reflectDir = reflect(-lightDir, vertNormal);
        	//float spec = pow(max(dot(viewDir, reflectDir), 0.0), 8);
        //blinn-phong
        float spec = pow(max(dot(normalize(vertNormal), normalize(halfwayDir)), 0.0), 64);
        vec3 specular = specularStrength * spec * lightColor;

        //shadow
        float shadow = ShadowCalculation(coordLight);

		//spotlight
		if(spotlight){
			float theta = dot(outSpotDir, normalize(-spotDir));
			float epsilon   = 1.0 - 0.95;
			float intensity = clamp((theta - 0.95) / epsilon, 0.0, 1.0);
			diffuse  *= intensity;
			specular *= intensity;
		}

        //attenuation
        float attenuation = 1.0 / (constantAttenuation + linearAttenuation * lightDistance + quadraticAttenuation * (lightDistance * lightDistance));

        //final mix
        ambient  *= attenuation;
        diffuse  *= attenuation;
        specular *= attenuation;
		verLighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * color;

        //light bulb don't cast shadow
        if(lightBulb){
            verLighting = objectColor/255;
        }
    }
}