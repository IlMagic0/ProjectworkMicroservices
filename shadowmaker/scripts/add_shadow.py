import sys
import cv2
import numpy as np

def add_stretched_shadow_same_size(image_path, output_path, stretch=30, opacity=100, blur_radius=5, offset_x=0):
    """
    Add a smooth stretched shadow to an image with alpha channel.

    Parameters:
        image_path: Input image path (must have alpha channel)
        output_path: Output image path
        stretch: Length of the shadow stretch (in pixels)
        opacity: Maximum opacity of the shadow (0-255)
        blur_radius: Radius for Gaussian blur to smooth the shadow
        offset_x: Horizontal offset for the shadow (positive values move shadow right)
    """
    img = cv2.imread(image_path, cv2.IMREAD_UNCHANGED)
    if img is None:
        raise ValueError("Could not read the image.")
    if img.shape[2] != 4:
        raise ValueError("Image must have an alpha channel.")

    h, w = img.shape[:2]
    alpha = img[:, :, 3]

    # Create shadow layer
    shadow_layer = np.zeros((h, w, 4), dtype=np.uint8)

    # Find all non-transparent pixels in each column
    for x in range(w):
        column = alpha[:, x]
        non_transparent_indices = np.where(column > 0)[0]
        if non_transparent_indices.size == 0:
            continue

        bottom_y = non_transparent_indices[-1]
        y1 = bottom_y
        y2 = min(bottom_y + stretch, h)

        # Calculate fade with smoother transition
        for y in range(y1, y2):
            # Cubic easing function for smoother fade
            t = (y - y1) / stretch
            fade = 1 - t * t * (3 - 2 * t)  # Smoothstep function
            shadow_opacity = int(opacity * fade)

            # Apply horizontal offset if specified
            shadow_x = x + offset_x
            if 0 <= shadow_x < w:
                shadow_layer[y, shadow_x] = [0, 0, 0, shadow_opacity]

    # Apply Gaussian blur to soften the shadow
    if blur_radius > 0:
        # Blur only the alpha channel
        alpha_channel = shadow_layer[:, :, 3]
        alpha_blurred = cv2.GaussianBlur(alpha_channel, (2*blur_radius+1, 2*blur_radius+1), 0)

        # Ensure we don't exceed the original opacity
        alpha_blurred = np.minimum(alpha_blurred, alpha_channel)
        shadow_layer[:, :, 3] = alpha_blurred

    # Composite the shadow with the original image
    combined = img.copy()
    alpha_shadow = shadow_layer[:, :, 3:] / 255.0
    for c in range(3):
        combined[:, :, c] = (1 - alpha_shadow[:, :, 0]) * combined[:, :, c] + alpha_shadow[:, :, 0] * shadow_layer[:, :, c]

    combined[:, :, 3] = np.clip(combined[:, :, 3] + shadow_layer[:, :, 3], 0, 255)

    cv2.imwrite(output_path, combined)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python add_shadow.py <input_path> <output_path> [stretch] [opacity] [blur_radius] [offset_x]")
        print("Example: python add_shadow.py input.png output.png 30 100 5 2")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    # Default parameters
    stretch = 30
    opacity = 150
    blur_radius = 5
    offset_x = 0

    # Parse optional parameters
    if len(sys.argv) > 3:
        stretch = int(sys.argv[3])
    if len(sys.argv) > 4:
        opacity = int(sys.argv[4])
    if len(sys.argv) > 5:
        blur_radius = int(sys.argv[5])
    if len(sys.argv) > 6:
        offset_x = int(sys.argv[6])

    try:
        add_stretched_shadow_same_size(
            input_path, output_path,
            stretch=stretch,
            opacity=opacity,
            blur_radius=blur_radius,
            offset_x=offset_x
        )
        sys.exit(0)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)