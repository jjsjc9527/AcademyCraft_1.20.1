#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 result = texture(Sampler0, texCoord0) * ColorModulator;
    float c = (result.r + result.g + result.b) / 3.0;
    fragColor = vec4(c, c, c, result.a);
}
