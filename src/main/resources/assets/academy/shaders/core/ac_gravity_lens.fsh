#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec2 SegA;
uniform vec2 SegB;
uniform vec4 LensParam;

out vec4 fragColor;

void main() {

    if (ScreenSize.x < 0.5 || ScreenSize.y < 0.5) {
        discard;
    }

    vec2 p = gl_FragCoord.xy;

    vec2 ab = SegB - SegA;
    float t = clamp(dot(p - SegA, ab) / max(dot(ab, ab), 1.0e-4), 0.0, 1.0);
    vec2 axis = SegA + t * ab;
    vec2 d = p - axis;
    float dist = length(d);

    float R = LensParam.x;
    if (dist > R) {
        discard;
    }

    float f = 1.0 - dist / R;
    float pull = f * f;

    vec2 dir = dist > 0.5 ? d / dist : vec2(0.0);
    vec2 uv = (p - dir * LensParam.y * pull) / ScreenSize;

    uv = clamp(uv, vec2(0.001), vec2(0.999));

    vec3 col = texture(Sampler0, uv).rgb;

    col += vec3(LensParam.w * pull * pull);

    fragColor = vec4(col, LensParam.z);
}
