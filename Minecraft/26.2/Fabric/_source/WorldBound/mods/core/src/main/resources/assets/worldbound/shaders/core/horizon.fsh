#version 330

// WorldBound horizon: vanilla's own fog (weather/cave/water, then the render-distance fade that the mod
// has pushed out to the horizon range), so the join with real chunks has the same colour.

#moj_import <minecraft:fog.glsl>

layout(std140) uniform HorizonInfo {
    vec4 Params;
    vec4 Mask;
};

uniform sampler2D ChunkMask; // one texel per chunk: 1 where Minecraft draws the real chunk

in float sphericalDistance;
in float cylindricalDistance;
in float groundDistance;
in vec2 maskBlock;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // Nothing where Minecraft draws the real chunk (so dug and blasted holes stay holes); without a mask, nothing
    // inside the inner radius.
    if (groundDistance < Params.x) discard;
    if (Mask.w > 0.5) {
        ivec2 c = ivec2(floor(maskBlock / 16.0));
        int n = int(Mask.z + 0.5);
        if (c.x >= 0 && c.y >= 0 && c.x < n && c.y < n && texelFetch(ChunkMask, c, 0).r > 0.5) discard;
    }
    fragColor = apply_fog(vertexColor, sphericalDistance, cylindricalDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
