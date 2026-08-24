#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const vec2 origSize = vec2(964, 147);
const vec2 iconOffset = vec2(857, 43);
const vec2 iconMul = vec2(1.0 / 65, 1.0 / 65);

void main() {
    vec2 temp = ((texCoord0 * origSize) - iconOffset) * iconMul;

    float maskColor;
    if (temp.s < 0 || temp.s > 1 || temp.t < 0 || temp.t > 1) {
        maskColor = 1;
    } else {
        maskColor = 1 - texture(Sampler1, temp.st).a;
    }

    vec4 texColor = texture(Sampler0, texCoord0.st);
    fragColor = vertexColor * vec4(texColor.rgb, texColor.a * maskColor) * ColorModulator;
}
