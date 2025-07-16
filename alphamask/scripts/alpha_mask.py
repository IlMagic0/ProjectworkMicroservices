# generate_alpha_mask.py

import sys
from PIL import Image

def generate_alpha_mask(input_path, output_path):
    try:
        img = Image.open(input_path).convert("RGBA")
        alpha = img.getchannel("A")
        alpha.save(output_path)
        print(f"Alpha mask saved: {output_path}")
        return 0
    except Exception as e:
        print(f"Error: {e}")
        return 1

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python generate_alpha_mask.py <input_path> <output_path>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    sys.exit(generate_alpha_mask(input_path, output_path))
