import cv2
import numpy as np
import math
import sys
import json

def calculate_car_status(input_path):
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

    # Calculate tilt angle
    dx = (x_right - x_left)
    dy = (y_left - y_right)
    angle_radians = math.atan2(dy, dx)
    angle_degrees = math.degrees(angle_radians)

    # Determine status and direction
    if angle_degrees > 4:        # left side higher
        status = "Inclined"
        direction = 1
    elif angle_degrees < -4:     # right side higher
        status = "Inclined"
        direction = 0
    else:
        status = "Flat"
        direction = 0  # irrelevant when flat

    # Print JSON result
    print(json.dumps({
        "status": status,
        "direction": direction
    }))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(json.dumps({"error": "Usage: python get_car_orientation.py <input_path>"}))
        sys.exit(1)

    input_path = sys.argv[1]
    calculate_car_status(input_path)
