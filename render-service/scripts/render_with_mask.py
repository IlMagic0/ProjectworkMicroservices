import sys
import bpy
from mathutils import Vector
import os

# --------------------------------------------------
# CLI args after '--': <color> <mask> <output>
# --------------------------------------------------
argv = sys.argv
if "--" not in argv or len(argv[argv.index("--") + 1:]) != 3:
    print("Usage: blender -b <blend> -P render_with_mask.py -- <colorPNG> <maskPNG> <outputPNG>")
    sys.exit(1)

color_image_path, alpha_mask_path, output_path = argv[argv.index("--") + 1:]

# Absolute path to current .blend (already opened by blender -b <blend>)
blend_dir = os.path.dirname(bpy.data.filepath)
print(f"[INFO] Blend dir: {blend_dir}")

# --------------------------------------------------
# Load images
# --------------------------------------------------
color_img = bpy.data.images.load(color_image_path)
alpha_img = bpy.data.images.load(alpha_mask_path)
w, h = color_img.size
aspect = w / h

# --------------------------------------------------
# Add plane sized to image
# --------------------------------------------------
bpy.ops.mesh.primitive_plane_add(size=1, location=(0, 0, 0))
bpy.context.object.visible_diffuse = False
plane = bpy.context.active_object
plane_height = 2.0
plane.scale = Vector((aspect * plane_height, plane_height, 1))
plane.location.z += plane_height / 3.8

# --------------------------------------------------
# Build material
# --------------------------------------------------
mat = bpy.data.materials.new(name="MaskedMaterial")
mat.use_nodes = True
nodes = mat.node_tree.nodes
links = mat.node_tree.links
nodes.clear()

output = nodes.new("ShaderNodeOutputMaterial")
bsdf   = nodes.new("ShaderNodeBsdfPrincipled")
img_color = nodes.new("ShaderNodeTexImage")
img_alpha = nodes.new("ShaderNodeTexImage")

img_color.image = color_img
img_alpha.image = alpha_img

img_color.interpolation = 'Smart'
img_alpha.interpolation = 'Smart'

# Position nodes (just for readability in UI)
output.location = (400, 0)
bsdf.location   = (200, 0)
img_color.location = (-400, 100)
img_alpha.location = (-400, -100)

links.new(img_color.outputs["Color"], bsdf.inputs["Base Color"])
links.new(img_alpha.outputs["Color"], bsdf.inputs["Alpha"])
links.new(bsdf.outputs["BSDF"], output.inputs["Surface"])

mat.blend_method = 'BLEND'
plane.data.materials.clear()
plane.data.materials.append(mat)

# --------------------------------------------------
# Point plane to camera
# --------------------------------------------------
cam = next((o for o in bpy.data.objects if o.type == 'CAMERA'), None)
if cam:
    direction = cam.location - plane.location
    plane.rotation_euler = direction.to_track_quat('-Z', 'Y').to_euler()

# --------------------------------------------------
# Render settings
# --------------------------------------------------
scene = bpy.context.scene
scene.render.engine = 'CYCLES'
scene.cycles.samples = 10
scene.render.filepath = output_path

# Try GPU
prefs = bpy.context.preferences
if 'cycles' in prefs.addons:
    cprefs = prefs.addons['cycles'].preferences
    if 'CUDA' in cprefs.get_device_types(bpy.context):
        cprefs.compute_device_type = 'CUDA'
        scene.cycles.device = 'GPU'
        for d in cprefs.devices:
            d.use = True

# --------------------------------------------------
# Render!
# --------------------------------------------------
bpy.ops.render.render(write_still=True)
print(f"[OK] Render saved to {output_path}")