import cv2
import numpy as np
import math
import sys
import json

def get_car_orientation(image_path):
    img = cv2.imread(image_path, cv2.IMREAD_UNCHANGED)

    if img is None or img.shape[2] != 4:
        return {"error": "Immagine non valida o senza canale alpha"}

    alpha = img[:, :, 3]
    _, mask = cv2.threshold(alpha, 1, 255, cv2.THRESH_BINARY)

    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return {"error": "Nessun oggetto trovato"}

    largest = max(contours, key=cv2.contourArea)
    if len(largest) < 5:
        return {"error": "Contorno troppo piccolo"}

    ellipse = cv2.fitEllipse(largest)
    angle = ellipse[2]
    x, y, w, h = cv2.boundingRect(largest)
    aspect_ratio = w / h

    pose = "Frontale" if 75 <= angle <= 105 else "Inclinata"

    return {
        "pose": pose,
        "inclinazione_angolo": round(angle, 2),
        "aspect_ratio": round(aspect_ratio, 2)
    }

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Nessun file passato"}))
        sys.exit(1)
    path = sys.argv[1]
    result = get_car_orientation(path)
    print(json.dumps(result))
