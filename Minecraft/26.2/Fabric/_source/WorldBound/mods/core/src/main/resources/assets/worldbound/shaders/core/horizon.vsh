#version 330

// WorldBound horizon: the distant-terrain column mesh. Positions are tile-local; ModelOffset moves the tile
// into camera space (the camera is the origin), so even 100 km out the floats stay precise.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

layout(std140) uniform HorizonInfo {
    vec4 Params; // x = radius with no horizon at all (only when there is no chunk mask), y = radius inside which the mesh is sunk, z = by how many blocks
    vec4 Mask;   // xy = the chunk mask's low corner relative to the camera (blocks), z = its size in chunks, w = 1 when in use
};

uniform sampler2D Sampler2;

in vec3 Position;
in vec4 Color;

out float sphericalDistance;
out float cylindricalDistance;
out float groundDistance;
out vec2 maskBlock;
out vec4 vertexColor;

void main() {
    vec3 pos = Position + ModelOffset;
    groundDistance = length(pos.xz);
    maskBlock = pos.xz - Mask.xy;
    if (groundDistance < Params.y) pos.y -= Params.z;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    sphericalDistance = fog_spherical_distance(pos);
    cylindricalDistance = fog_cylindrical_distance(pos);
    // Full daylight from the lightmap (block 0, sky 15): day/night, night vision and gamma for free.
    vec4 light = texture(Sampler2, vec2(0.5 / 16.0, 15.5 / 16.0));
    vertexColor = vec4(Color.rgb * light.rgb, 1.0);
}
