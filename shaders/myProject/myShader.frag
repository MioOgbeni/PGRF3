#version 150
#define M_PI 3.14159265359

in vec2 vertTexturePosition;

in vec3 vertColor; // input from the previous pipeline stage
in vec3 vertNormal;
in vec3 vertPosition;
in vec4 coordLight;

in vec3 lightDir;
in vec3 viewDir;
in vec3 halfwayDir;
in float lightDistance;

in vec3 outSpotDir;

in vec3 verLighting;

uniform vec3 lightPos;
uniform vec3 viewPos;

uniform float surfaceType;
uniform vec3 objectColor;
uniform bool ascii;
uniform bool moon;
uniform bool spotlight;
uniform bool coordsInTexture;
uniform bool lightBulb;

uniform vec3 spotDir;

uniform sampler2D mainTex;
uniform sampler2D mainHighTex;
uniform sampler2D moonTex;
uniform sampler2D moonHighTex;
uniform sampler2D shadowMap;

uniform float lightType;

out vec4 outColor; // output from the fragment shader

const float constantAttenuation = 1.0;
const float linearAttenuation = 0.14;
const float quadraticAttenuation = 0.07;

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
    vec3 normal = vertNormal;
    if(lightType == 1) {
        outColor = vec4(verLighting, 1.0);

    }else{
        vec3 color;

        if (surfaceType == 1){
            // texture
            if(!moon){
                if(coordsInTexture){
                    normal = texture2D(mainHighTex, vertTexturePosition.xy).xyz;
                    normal *= 2;
                    normal -= 1;
                    color =  texture(mainTex, vertTexturePosition.xy).rgb;
                }else{
                    normal = texture2D(mainHighTex, vertPosition.xy).xyz;
                    normal *= 2;
                    normal -= 1;
                    color =  texture(mainTex, vertPosition.xy).rgb;
                }
            }else{
                if(coordsInTexture){
                    normal = texture2D(moonHighTex, vertTexturePosition.xy).xyz;
                    normal *= 2;
                    normal -= 1;
                    color =  texture(moonTex, vertTexturePosition.xy).rgb;
                }else{
                    normal = texture2D(moonHighTex, vertPosition.xy).xyz;
                    normal *= 2;
                    normal -= 1;
                    color =  texture(moonTex, vertPosition.xy).rgb;
                }
            }
        } else if (surfaceType == 2){
            // normal color
            float cosAlpha = dot(normalize(normal), normalize(lightDir));
            color =  vec3(cosAlpha);
        } else if (surfaceType == 3){
            // xyz color
            color = vertColor;
        } else if (surfaceType == 4){
            // depth color
            color = gl_FragCoord.zzz;
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
        float diff = max(dot(normalize(normal), normalize(lightDir)), 0.0);
        vec3 diffuse = diff * lightColor;

        //specular
        //phong
            //vec3 reflectDir = reflect(-lightDir, vertNormal);
            //float spec = pow(max(dot(viewDir, reflectDir), 0.0), 8);
        //blinn-phong
        float spec = pow(max(dot(normalize(normal), normalize(halfwayDir)), 0.0), 64);
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
        vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * color;

        // ascii rendering
        int n;
        vec2 p;
        if(ascii){
            float gray = 0.3 * lighting.r + 0.59 * lighting.g + 0.11 * lighting.b;

            //bitmap generated in http://www.thrill-project.com/archiv/coding/bitmap/
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

            if(ascii){
                outColor = vec4(objectColor/255 * character(n, p), 1.0);
            }else{
                outColor = vec4(objectColor/255, 1.0);
            }
        }
    }
}