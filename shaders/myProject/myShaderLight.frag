#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 normal_IO;
in vec3 lightVec;
in vec3 eyeVec;
in vec2 outPosition;

uniform float lightType;
uniform float renderTexture;
uniform float surfaceType;
uniform float testingShader;

uniform sampler2D mainTex;
uniform sampler2D normTex;
uniform sampler2D heightTex;


out vec4 outColor; // output from the fragment shader

void main() {
    if(testingShader == 1){

        if (surfaceType == 1){
            outColor = vec4(texture(mainTex, outPosition).rgb, 1.0);
        }else if (surfaceType == 2){
            float cosAlpha = dot(normal_IO, normalize(lightVec));
            outColor = vec4(vec3(cosAlpha), 1.0);
        }else{
            outColor = vec4(1.0, 0.0, 0.0, 1.0);
        }


    }else if (testingShader == 0){

        vec3 norm = normal_IO;
        vec2 texCoord = outPosition;

        if (lightType == 0) {
            outColor = vec4(vertColor, 1.0);
            if (renderTexture == 1){
                outColor = vec4(texture2D(mainTex, outPosition).rgb * vertColor,1);
            } else {
                outColor = vec4(vertColor, 1.0);
            }
        } else {

            if (renderTexture == 1){
                // normal mapping
                norm = texture2D(normTex, texCoord).xyz;
                norm *= 2;
                norm -= 1;
            }

            float diff = dot(norm, lightVec);
            diff = max(0,diff);
            vec3 halfVec = normalize(normalize(eyeVec) + lightVec);
            float spec = dot(norm, halfVec);
            spec = max(0,spec);
            spec = pow(spec,10);
            float ambient = 0.1;
            vec3 diffColor;
            if (renderTexture == 1) {
                diffColor = texture2D(mainTex, texCoord).rgb;
            } else {
                diffColor = vec3(texCoord, 0);
            }

            vec3 fragColor = diffColor * (min(ambient + diff,1)) + vec3(1,1,1) * spec;

            outColor=vec4(fragColor,1);

        }
    }
}