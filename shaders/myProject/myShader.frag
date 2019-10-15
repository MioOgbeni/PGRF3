#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 normal_IO;
in vec3 lightVec;
in vec3 eyeVec;

out vec4 outColor; // output from the fragment shader

void main() {
    //float cosAlpha = dot(normal_IO, normalize(lightVec));
	//outColor = vec4(vec3(cosAlpha), 1.0);
	//outColor = vec4(vertColor, 1.0);

	float diff = dot(normal_IO, lightVec);
    diff = max(0,diff);
    vec3 halfVec = normalize(normalize(eyeVec) + lightVec);
    float spec = dot(normal_IO, halfVec);
    spec = max(0,spec);
    spec = pow(spec,10);
    float ambient = 0.1;
    vec3 diffColor;

    vec3 fragColor = diffColor * (min(ambient + diff,1)) + vec3(1,1,1) * spec;

    outColor=vec4(fragColor,1);
}