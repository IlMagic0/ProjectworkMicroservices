"""
CLI:  python infer_yolov8.py <input.png|jpg> <output_mask.png>
Model checkpoint `best.pt` must sit in the same `scripts` folder.
"""
import sys
from ultralytics import YOLO
import cv2
import numpy as np
from pathlib import Path

if len(sys.argv) != 3:
    print("Usage: python infer_yolov8.py <input_img> <output_mask>")
    sys.exit(1)

img_path  = Path(sys.argv[1])
out_path  = Path(sys.argv[2])
model_pt  = Path(__file__).with_name("best.pt")

if not model_pt.exists():
    print(f"[ERROR] Model not found: {model_pt}")
    sys.exit(1)

img = cv2.imread(str(img_path))
if img is None:
    print("[ERROR] cannot load image:", img_path)
    sys.exit(1)

H, W, _ = img.shape
model = YOLO(str(model_pt))
results = model(img, conf=0.1, verbose=False)   # no popup window

# Build union‐of‐masks
combined = np.zeros((H, W), dtype=np.uint8)
for r in results:
    if r.masks is None:
        continue
    masks = np.stack([m.numpy() for m in r.masks.data], axis=0)
    combined = np.maximum(combined, (np.max(masks, axis=0) * 255).astype(np.uint8))

cv2.imwrite(str(out_path), combined)
print(f"[OK] Mask saved to {out_path}")
sys.exit(0)
