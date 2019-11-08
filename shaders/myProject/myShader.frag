#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 vertNormal;
in vec3 vertPosition;
in vec4 coordLight;

uniform vec3 lightPos;
uniform vec3 viewPos;

uniform float surfaceType;
uniform vec3 objectColor;

uniform sampler2D mainTex;
uniform sampler2D normTex;
uniform sampler2D heightTex;
uniform sampler2D shadowMap;

uniform bool lightBulb;

out vec4 outColor; // output from the fragment shader

float ShadowCalculation(vec4 coordLight)
{
    vec3 projCoords = coordLight.xyz / coordLight.w;
    projCoords = projCoords * 0.5 + 0.5;
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
    float bias = 0.005;

    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x)
    {
        for(int y = -1; y <= 1; ++y)
        {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    return shadow;
}

void main() {

    vec3 color;

    if (surfaceType == 1){
        // barva textura
        color =  texture(mainTex, vertPosition.xy).rgb;
    } else if (surfaceType == 2){
        // barva normála
        float cosAlpha = dot(vertNormal, normalize(vertNormal));
        color =  vec3(cosAlpha);
    } else if (surfaceType == 3){
        // barva xyz
        color = vertColor;
    } else if (surfaceType == 4){
        // barva hloubka
        color = gl_FragCoord.zzz;
    }else{
        //  barva solid z cpu
        color = objectColor/255;
    }

    vec3 normal = normalize(vertNormal);
    vec3 lightColor = vec3(1.0);
    // ambient
    vec3 ambient = 0.15 * color;
    // diffuse
    vec3 lightDir = normalize(lightPos - vertPosition);
    float diff = max(dot(lightDir, normal), 0.0);
    vec3 diffuse = diff * lightColor;
    // specular
    vec3 viewDir = normalize(viewPos - vertPosition);
    float spec = 0.0;
    vec3 halfwayDir = normalize(lightDir + viewDir);
    spec = pow(max(dot(normal, halfwayDir), 0.0), 64.0);
    vec3 specular = spec * lightColor;
    // calculate shadow
    float shadow = ShadowCalculation(coordLight);
    // mix light, shadow and color
    vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * color;

    outColor = vec4(lighting, 1.0);

    //light bulb don't cast shadow
    if(lightBulb){
        outColor = vec4(objectColor, 1.0);
    }
}