#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float TexOffset;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 colorTex = vertexColor * texture(Sampler0, texCoord0.st + vec2(TexOffset, 0));
    float colorMask = texture(Sampler1, texCoord0.st).a;
    fragColor = vec4(colorTex.rgb, colorMask * colorTex.a) * ColorModulator;
}
