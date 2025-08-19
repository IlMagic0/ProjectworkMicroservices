import sys
import bpy
from mathutils import Vector
import os

# --------------------------------------------------
# CLI args after '--': <color> <mask> <windowMask> <output> <bool>
# --------------------------------------------------
argv = sys.argv
if "--" not in argv or len(argv[argv.index("--") + 1:]) != 5:
    print("Usage: blender -b <blend> -P render_with_mask.py -- <colorPNG> <maskPNG> <windowPNG> <bool> <outputPNG>")
    sys.exit(1)

color_image_path, alpha_mask_path, window_mask_path, rotate_flag, output_path = argv[argv.index("--") + 1:]
rotate_flag = int(rotate_flag)  # 0 or 1

# Absolute path to current .blend (already opened by blender -b <blend>)
blend_dir = os.path.dirname(bpy.data.filepath)
print(f"[INFO] Blend dir: {blend_dir}")

# --------------------------------------------------
# Rotate Cube depending on boolean
# --------------------------------------------------
cube = bpy.data.objects.get("Cube")
if cube:
    cube.rotation_euler.z = 10 * (1 - 2*rotate_flag) * (3.14159265 / 180)  # degrees -> radians
    print(f"[INFO] Cube rotated to {cube.rotation_euler.z} radians")
    # Move along local axes after rotation
    if cube.rotation_euler.z > 0:
        # Move -1m along local X
        cube.location += cube.matrix_world.to_quaternion() @ Vector((-2.5, 1, 0))
    else:
        # Move 1m along local Y
        cube.location += cube.matrix_world.to_quaternion() @ Vector((-1, 2.5, 0))
else:
    print("[WARN] Cube object not found")

# --------------------------------------------------
# Load images
# --------------------------------------------------
color_img = bpy.data.images.load(color_image_path)
alpha_img = bpy.data.images.load(alpha_mask_path)
window_img = bpy.data.images.load(window_mask_path)
w, h = color_img.size
aspect = w / h

# --------------------------------------------------
# Add plane sized to image
# --------------------------------------------------
bpy.ops.mesh.primitive_plane_add(size=1, location=(0, 0, 0))

# Disable ray visibility except camera
bpy.context.object.visible_diffuse = False
bpy.context.object.visible_shadow = False
bpy.context.object.visible_volume_scatter = False
bpy.context.object.visible_transmission = False
bpy.context.object.visible_glossy = False
bpy.context.object.visible_camera = True

plane = bpy.context.active_object
plane_height = 2.0
plane.scale = Vector((aspect * plane_height, plane_height, 1))
plane.location.z += plane_height / 3.8
plane.scale.x *= -1







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
img_window = nodes.new("ShaderNodeTexImage")
mix_mask = nodes.new("ShaderNodeMath")

# Set images
img_color.image = color_img
img_alpha.image = alpha_img
img_window.image = window_img

img_color.interpolation = 'Smart'
img_alpha.interpolation = 'Smart'
img_window.interpolation = 'Smart'

# Math node to combine the two alpha masks (Multiply = intersection)
mix_mask.operation = 'MULTIPLY'

# Position nodes
output.location = (400, 0)
bsdf.location   = (200, 0)
img_color.location = (-600, 200)
img_alpha.location = (-600, -50)
img_window.location = (-600, -250)
mix_mask.location = (-300, -100)

# Links
links.new(img_color.outputs["Color"], bsdf.inputs["Base Color"])
links.new(img_alpha.outputs["Color"], mix_mask.inputs[0])
links.new(img_window.outputs["Color"], mix_mask.inputs[1])
links.new(mix_mask.outputs[0], bsdf.inputs["Alpha"])
links.new(bsdf.outputs["BSDF"], output.inputs["Surface"])

bsdf.inputs["Roughness"].default_value = 0.8  # value between 0 (smooth) and 1 (rough)


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
