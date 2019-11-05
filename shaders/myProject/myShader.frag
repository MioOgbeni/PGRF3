#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 vertNormal;
in vec2 vertPosition;
in vec4 coordLight;

uniform float surfaceType;
uniform vec3 objectColor;

uniform sampler2D mainTex;
uniform sampler2D normTex;
uniform sampler2D heightTex;
uniform sampler2D shadowMap;

out vec4 outColor; // output from the fragment shader

void main() {
    if (surfaceType == 1){
        // barva textura
        outColor = vec4(texture(mainTex, vertPosition).rgb, 1.0);
    } else if (surfaceType == 2){
        // barva normála
        float cosAlpha = dot(vertNormal, normalize(vertNormal));
        outColor = vec4(vec3(cosAlpha), 1.0);
    } else if (surfaceType == 3){
        // barva xyz
        outColor = vec4(vertColor,0);
    } else if (surfaceType == 4){
        // barva hloubka
        outColor = vec4(vec3(gl_FragCoord.zzz), 1.0);
    }else{
        // barva solid z cpu
        if (texture(shadowMap, coordLight.xy).z < coordLight.z)
        {
            outColor = vec4(1, 1, 1, 1.0);
        } else {
            outColor = vec4(objectColor.r, objectColor.g, objectColor.b, 1.0);
        }
    }
}