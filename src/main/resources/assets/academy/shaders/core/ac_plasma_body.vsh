#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 camspace;
out vec2 boardUV;

void main() {
    vec4 mv = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * mv;
    camspace = mv.xyz / mv.w;
    boardUV = UV0;
}
