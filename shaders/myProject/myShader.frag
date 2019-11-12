#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec3 vertNormal;
in vec3 vertPosition;
in vec4 coordLight;

in vec3 lightDir;
in vec3 viewDir;
in vec3 halfwayDir;

uniform vec3 lightPos;
uniform vec3 viewPos;

uniform float surfaceType;
uniform vec3 objectColor;
uniform float ascii;

uniform sampler2D mainTex;
uniform sampler2D normTex;
uniform sampler2D heightTex;
uniform sampler2D shadowMap;

uniform bool lightBulb;

uniform float lightType;

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
    for(int x = -3; x <= 3; ++x)
    {
        for(int y = -3; y <= 3; ++y)
        {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 49.0;

    return shadow;
}

float character(int n, vec2 p)
{
    p = floor(p*vec2(4.0, -4.0) + 2.5);
    if (clamp(p.x, 0.0, 4.0) == p.x)
    {
        if (clamp(p.y, 0.0, 4.0) == p.y)
        {
            int a = int(round(p.x) + 5.0 * round(p.y));
            if (((n >> a) & 1) == 1) return 1.0;
        }
    }
    return 0.0;
}

void main() {
    if(lightType == 0) {
        outColor = vec4(1.0);
    }else{
        vec3 color;

        if (surfaceType == 1){
            // barva textura
            color =  texture(mainTex, vertPosition.xy).rgb;
        } else if (surfaceType == 2){
            // barva normála
            float cosAlpha = dot(vertNormal, normalize(lightPos));
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

        //light parameters declaration
        float ambientStrength = 0.2;
        float specularStrength = 0.5;
        vec3 lightColor = vec3(1.0);

        //ambient
        vec3 ambient = ambientStrength * lightColor;

        //difuse
        float diff = max(dot(vertNormal, lightDir), 0.0);
        vec3 diffuse = diff * lightColor;

        //specular
        //phong
            //vec3 reflectDir = reflect(-lightDir, vertNormal);
            //float spec = pow(max(dot(viewDir, reflectDir), 0.0), 8);
        //blinn-phong
        float spec = pow(max(dot(vertNormal, halfwayDir), 0.0), 64);
        vec3 specular = specularStrength * spec * lightColor;

        //shadow
        float shadow = ShadowCalculation(coordLight);

        //final mix
        vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * color;

        int n;
        vec2 p;
        if(ascii == 1.0){
            float gray = 0.3 * lighting.r + 0.59 * lighting.g + 0.11 * lighting.b;

            n =  4096;                // .
            if (gray > 0.2) n = 65600;    // :
            if (gray > 0.3) n = 332772;   // *
            if (gray > 0.4) n = 15255086; // o
            if (gray > 0.5) n = 23385164; // &
            if (gray > 0.6) n = 15252014; // 8
            if (gray > 0.7) n = 13199452; // @
            if (gray > 0.8) n = 11512810; // #

            p = mod(gl_FragCoord.xy/4.0, 2.0) - vec2(1.0);

            lighting = lighting * character(n, p);
        }

        outColor = vec4(lighting, 1.0);

        //light bulb don't cast shadow
        if(lightBulb){

            if(ascii == 1.0){
                outColor = vec4(objectColor * character(n, p), 1.0);
            }else{
                outColor = vec4(objectColor, 1.0);
            }
        }
    }
}