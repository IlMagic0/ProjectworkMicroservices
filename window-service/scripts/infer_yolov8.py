"""
CLI usage:
python infer_yolov8.py <input_img> <output_img>
Model checkpoint `best.pt` must be in the same `scripts` folder as this script.
"""

import sys
from pathlib import Path
import cv2
import numpy as np
from ultralytics import YOLO

# ----------------------------
# CLI arguments
# ----------------------------
if len(sys.argv) != 3:
    print("Usage: python infer_yolov8.py <input_img> <output_img>")
    sys.exit(1)

input_path = sys.argv[1]
output_path = sys.argv[2]

# ----------------------------
# Paths
# ----------------------------
script_dir = Path(__file__).parent
model_path = script_dir / "best.pt"

# ----------------------------
# Load image
# ----------------------------
img = cv2.imread(input_path)
if img is None:
    print(f"[ERROR] Failed to load image: {input_path}")
    sys.exit(1)

H, W, _ = img.shape
print(f"[INFO] Image loaded: {img.shape}")

# ----------------------------
# Load model and run detection
# ----------------------------
if not model_path.exists():
    print(f"[ERROR] Model not found: {model_path}")
    sys.exit(1)

model = YOLO(str(model_path))
results = model(img, conf=0.1, show=False)

# ----------------------------
# Process results
# ----------------------------
for result in results:
    if result.masks is not None:
        all_masks = np.stack([mask.numpy() for mask in result.masks.data], axis=0)
        combined_mask = np.max(all_masks, axis=0) * 255
        combined_mask = cv2.resize(combined_mask, (W, H)).astype(np.uint8)

        # Binary mask for islands
        binary_mask = (combined_mask > 127).astype(np.uint8)
        num_labels, labels = cv2.connectedComponents(binary_mask)

        # Convert image to grayscale
        gray_img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # Create white background output
        output = np.full((H, W, 3), 255, dtype=np.uint8)

        print(f"\n--- Detected {num_labels - 1} mask islands ---")
        for label in range(1, num_labels):
            island_mask = (labels == label)
            region_pixels = gray_img[island_mask]

            if region_pixels.size == 0:
                continue

            avg_brightness = np.mean(region_pixels)

            # Decide shading based on brightness
            if avg_brightness > 180:
                shade = 180    # very dark
            elif avg_brightness > 100:
                shade = 195   # medium gray
            else:
                shade = 210   # light gray

            print(f"Island {label}: Brightness = {avg_brightness:.2f}, Fill shade = {shade}")
            output[island_mask] = (shade, shade, shade)

        # Save the final image
        cv2.imwrite(output_path, output)
        print(f"\n[INFO] Shaded island image saved to: {output_path}")
    else:
        print("[WARN] No masks found. Returning full white image.")
        output = np.full((H, W, 3), 255, dtype=np.uint8)
        cv2.imwrite(output_path, output)
        print(f"[INFO] White image saved to: {output_path}")
