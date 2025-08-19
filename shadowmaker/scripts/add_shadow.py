import cv2
import numpy as np
import math
import sys
import json

def add_shadow(input_path, output_path):
    # Load image with alpha

    blur_radius = 10  # Adjust for softer shadow
    # Higher = softer shadow, lower = sharper shadow

    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    if img is None or img.shape[2] != 4:
        print(json.dumps({"error": "Input must be a valid RGBA image"}))
        sys.exit(1)

    alpha = img[:, :, 3]

    # Crop to bounding box of non-transparent pixels
    coords = cv2.findNonZero((alpha > 0).astype(np.uint8))
    x, y, w_crop, h_crop = cv2.boundingRect(coords)
    alpha_cropped = alpha[y:y+h_crop, x:x+w_crop]

    # Find lowest points on leftmost and rightmost edges
    points = np.column_stack(np.where(alpha_cropped > 0))
    left_region = points[points[:, 1] < w_crop * 0.2]
    y_left = np.max(left_region[:, 0])
    x_left = int(np.mean(left_region[left_region[:, 0] == y_left][:, 1]))

    right_region = points[points[:, 1] > w_crop * 0.8]
    y_right = np.max(right_region[:, 0])
    x_right = int(np.mean(right_region[right_region[:, 0] == y_right][:, 1]))

    # Compute tilt angle
    dx = (x_right - x_left)
    dy = (y_left - y_right)
    angle_degrees = math.degrees(math.atan2(dy, dx))

    # Compute center and size of ellipse
    center_x = x + int((x_left + x_right) / 2)
    center_y = y + int((y_left + y_right) / 2)
    length = math.hypot(dx, dy)
    height = length / 5

    # --- Create shadow layer ---
    shadow_layer = np.zeros_like(img, dtype=np.uint8)
    cv2.ellipse(
        shadow_layer,
        center=(center_x, center_y),
        axes=(int(length/2), int(height/2)),
        angle=-angle_degrees,
        startAngle=0,
        endAngle=360,
        color=(0, 0, 0, 255),
        thickness=-1
    )

    # Apply Gaussian blur for soft shadow
    shadow_layer[:, :, 3] = cv2.GaussianBlur(shadow_layer[:, :, 3], (0, 0), sigmaX=blur_radius, sigmaY=blur_radius)
    # Change these to adjust the darkness of the shadow
    shadow_layer[:, :, 3] = (shadow_layer[:, :, 3] * 0.7).astype(np.uint8)  # reduce opacity


    # --- Composite: shadow behind original ---
    combined = np.zeros_like(img, dtype=np.uint8)  # transparent canvas

    # Make a mask of where the car exists
    car_alpha = img[:, :, 3:4] / 255.0
    shadow_alpha = shadow_layer[:, :, 3:4] / 255.0

    # Shadow only where the car is NOT
    shadow_alpha_behind = shadow_alpha * (1 - car_alpha)

    for c in range(3):
        combined[:, :, c] = shadow_layer[:, :, c] * shadow_alpha_behind[:, :, 0]

    combined[:, :, 3] = (shadow_alpha_behind[:, :, 0] * 255).astype(np.uint8)

    # Overlay original image fully on top
    for c in range(3):
        combined[:, :, c] = img[:, :, c] * car_alpha[:, :, 0] + combined[:, :, c] * (1 - car_alpha[:, :, 0])

    combined[:, :, 3] = np.clip(img[:, :, 3] + combined[:, :, 3], 0, 255)

    cv2.imwrite(output_path, combined)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(json.dumps({"error": "Usage: python script.py <input_path> <output_path>"}))
        sys.exit(1)

    add_shadow(sys.argv[1], sys.argv[2])
