#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 normal_IO;
in vec3 lightVec;
in vec3 eyeVec;
in vec2 outPosition;

uniform sampler2D textureID;

out vec4 outColor; // output from the fragment shader

void main() {
   //float cosAlpha = max(0,dot(normal_IO, normalize(lightVec)));
	//outColor = vec4(vec3(cosAlpha), 1.0);
	//outColor = vec4(vertColor, 1.0);

	outColor = vec4(texture(textureID, outPosition).rgb, 1.0);
}