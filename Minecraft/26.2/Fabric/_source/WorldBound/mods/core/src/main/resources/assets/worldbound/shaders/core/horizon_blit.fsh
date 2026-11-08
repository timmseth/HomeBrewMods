#version 330

// WorldBound horizon composite: the horizon was drawn into its own colour+depth with a far projection; here it
// is laid under the real chunks by comparing true view distances, and its depth is rewritten in the scene's
// projection so clouds, water and particles still sort against the far mountains.
// 26.2 uses reversed depth: near = 1, far = 0, cleared to 0.

uniform sampler2D LodColor;
uniform sampler2D LodDepth;
uniform sampler2D SceneDepth;

layout(std140) uniform HorizonBlit {
    vec4 LodProj;    // x = m22, y = m32 of the horizon projection, z = 1 when clip z runs 0..1
    vec4 SceneProj;  // the same for the scene projection
};

in vec2 texCoord;

out vec4 fragColor;

float viewDistance(float depth, vec4 proj) {
    float ndc = proj.z > 0.5 ? depth : depth * 2.0 - 1.0;
    return proj.y / (ndc + proj.x);
}

float depthFor(float dist, vec4 proj) {
    float ndc = -proj.x + proj.y / dist;
    return proj.z > 0.5 ? ndc : ndc * 0.5 + 0.5;
}

void main() {
    float ld = texture(LodDepth, texCoord).r;
    if (ld <= 0.0) discard;                                            // no horizon here
    float dl = viewDistance(ld, LodProj);
    float sd = texture(SceneDepth, texCoord).r;
    if (sd > 0.0 && viewDistance(sd, SceneProj) <= dl) discard;        // real chunks are nearer
    fragColor = vec4(texture(LodColor, texCoord).rgb, 1.0);
    gl_FragDepth = clamp(depthFor(dl, SceneProj), 1e-7, 1.0);          // beyond the scene's far plane: just above "nothing"
}
