#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec4 ColorModulator;
uniform float Progress;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float threshold = texture(Sampler1, texCoord0).r;
    fragColor = Progress > threshold
        ? texture(Sampler0, texCoord0) * ColorModulator
        : vec4(0.0, 0.0, 0.0, 0.0);
}
