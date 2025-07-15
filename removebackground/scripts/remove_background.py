import sys
import io
from PIL import Image
from rembg import remove

def remove_background(input_path, output_path):
    try:
        print(f"[INFO] Opening image from: {input_path}")
        with open(input_path, 'rb') as input_file:
            input_data = input_file.read()

        print("[INFO] Removing background...")
        output_data = remove(input_data)

        print(f"[INFO] Saving result to: {output_path}")
        with open(output_path, 'wb') as output_file:
            output_file.write(output_data)

        print("[INFO] Done.")
        return 0

    except Exception as e:
        print(f"[ERROR] {e}")
        return 1

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("[ERROR] Usage: python remove_background.py <input_path> <output_path>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]
    print(f"[START] input: {input_path}, output: {output_path}")

    exit_code = remove_background(input_path, output_path)
    sys.exit(exit_code)
