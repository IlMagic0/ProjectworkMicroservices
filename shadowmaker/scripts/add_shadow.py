import sys
import cv2
import numpy as np

def add_stretched_shadow_same_size(image_path, output_path, stretch=30, opacity=100):
    img = cv2.imread(image_path, cv2.IMREAD_UNCHANGED)
    if img.shape[2] != 4:
        raise ValueError("Image must have an alpha channel.")

    h, w = img.shape[:2]
    alpha = img[:, :, 3]

    shadow_layer = np.zeros((h, w, 4), dtype=np.uint8)

    for x in range(w):
        column = alpha[:, x]
        non_transparent_indices = np.where(column > 0)[0]
        if non_transparent_indices.size == 0:
            continue
        bottom_y = non_transparent_indices[-1]

        y1 = bottom_y
        y2 = min(bottom_y + stretch, h)

        for y in range(y1, y2):
            fade = 1 - (y - y1) / stretch
            shadow_layer[y, x] = [0, 0, 0, int(opacity * fade)]

    combined = img.copy()
    alpha_shadow = shadow_layer[:, :, 3:] / 255.0
    for c in range(3):
        combined[:, :, c] = (1 - alpha_shadow[:, :, 0]) * combined[:, :, c] + alpha_shadow[:, :, 0] * shadow_layer[:, :, c]

    combined[:, :, 3] = np.clip(combined[:, :, 3] + shadow_layer[:, :, 3], 0, 255)

    cv2.imwrite(output_path, combined)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python add_shadow.py <input_path> <output_path>")
        sys.exit(1)
    input_path = sys.argv[1]
    output_path = sys.argv[2]

    try:
        add_stretched_shadow_same_size(input_path, output_path)
        sys.exit(0)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
