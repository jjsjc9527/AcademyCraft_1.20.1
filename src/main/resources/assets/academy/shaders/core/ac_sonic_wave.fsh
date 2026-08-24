#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float Distort;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float a = texture(Sampler0, texCoord0).a * vertexColor.a * ColorModulator.a;

    if (a < 0.004) {
        discard;
    }

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    float aL = texture(Sampler0, texCoord0 - vec2(texel.x, 0.0)).a;
    float aR = texture(Sampler0, texCoord0 + vec2(texel.x, 0.0)).a;
    float aD = texture(Sampler0, texCoord0 - vec2(0.0, texel.y)).a;
    float aU = texture(Sampler0, texCoord0 + vec2(0.0, texel.y)).a;
    vec2 grad = vec2(aR - aL, aU - aD);

    vec3 col;
    if (ScreenSize.x > 0.5 && ScreenSize.y > 0.5) {

        vec2 screenUV = gl_FragCoord.xy / ScreenSize;

        vec2 off = grad * Distort * a / ScreenSize;
        vec2 uv = clamp(screenUV + off, vec2(0.001), vec2(0.999));
        col = texture(Sampler1, uv).rgb;
    } else {

        col = vec3(1.0);
    }

    float spec = clamp(length(grad) * 2.6, 0.0, 1.0);
    col += vec3(spec * 0.35 * a);

    fragColor = vec4(col * ColorModulator.rgb, a);
}
