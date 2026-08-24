#version 150

uniform mat4 ModelViewMat;

uniform vec4 Ball0;
uniform vec4 Ball1;
uniform vec4 Ball2;
uniform vec4 Ball3;
uniform vec4 Ball4;
uniform vec4 Ball5;
uniform vec4 Ball6;
uniform vec4 Ball7;
uniform vec4 Ball8;
uniform vec4 Ball9;

uniform float Alpha;

in vec3 camspace;

in vec2 boardUV;

out vec4 fragColor;

const float EDGE_FADE = 0.62;

vec4 balls[10];

const vec3 PLASMA_COLD = vec3(0.32, 0.14, 0.62);

const vec3 PLASMA_HOT = vec3(0.96, 0.42, 1.00);

const float PLASMA_OVERSHOOT = 2.6;

float f(vec3 position) {
    float ret = 0.0;
    for (int i = 0; i < 10; ++i) {
        float distance = max(0.1, length(position - balls[i].xyz));
        ret += Alpha * balls[i].w / (distance * distance);
    }
    return clamp(ret, 0.0, 2.0);
}

vec3 plasmaColor(float density) {
    float t = clamp(density * 0.5, 0.0, 1.0);
    vec3 hue = mix(PLASMA_COLD, PLASMA_HOT, pow(t, 0.45));
    return mix(hue, vec3(PLASMA_OVERSHOOT), pow(t, 2.2));
}

vec4 rayMarch(vec3 begin, vec3 dir) {
    dir *= 0.72;

    vec3 pos = begin;

    vec4 accum = vec4(0.0, 0.0, 0.0, 0.0);

    for (int i = 0; i < 20; ++i) {
        float density = f(pos);

        float alpha = 0.18 * density;
        vec3 crl = plasmaColor(density);

        accum.rgb = mix(accum.rgb, crl, alpha / max(1.0e-6, accum.a + alpha));
        accum.a += alpha;

        pos += dir;
    }

    if (accum.a < 0.2) {
        accum.a = 2.0 * accum.a - 0.2;
    }
    return accum;
}

vec3 toCam(vec3 worldRelCam) {
    vec3 v = (ModelViewMat * vec4(worldRelCam, 1.0)).xyz;
    v.z = -v.z;
    return v;
}

void main() {
    balls[0] = vec4(toCam(Ball0.xyz), Ball0.w);
    balls[1] = vec4(toCam(Ball1.xyz), Ball1.w);
    balls[2] = vec4(toCam(Ball2.xyz), Ball2.w);
    balls[3] = vec4(toCam(Ball3.xyz), Ball3.w);
    balls[4] = vec4(toCam(Ball4.xyz), Ball4.w);
    balls[5] = vec4(toCam(Ball5.xyz), Ball5.w);
    balls[6] = vec4(toCam(Ball6.xyz), Ball6.w);
    balls[7] = vec4(toCam(Ball7.xyz), Ball7.w);
    balls[8] = vec4(toCam(Ball8.xyz), Ball8.w);
    balls[9] = vec4(toCam(Ball9.xyz), Ball9.w);

    vec3 cam = camspace;
    cam.z = -cam.z;

    vec3 dir = normalize(cam);

    vec4 rc = rayMarch(cam - dir * 7.2, dir);

    float r = length(boardUV - vec2(0.5)) * 2.0;
    float edge = 1.0 - smoothstep(EDGE_FADE, 1.0, r);

    rc.a = clamp(rc.a, 0.0, 1.0) * (0.5 + Alpha * 0.5) * edge;

    fragColor = rc;
}
