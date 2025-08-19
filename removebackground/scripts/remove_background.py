import sys
import io
from PIL import Image
from rembg import remove

def remove_background_and_center(input_path, output_path):
    try:
        print(f"[INFO] Opening image from: {input_path}")
        with open(input_path, 'rb') as input_file:
            input_data = input_file.read()

        print("[INFO] Removing background...")
        output_data = remove(input_data)

        # Load as PIL image
        img_no_bg = Image.open(io.BytesIO(output_data)).convert("RGBA")

        # Keep original size
        original_img = Image.open(input_path).convert("RGBA")
        original_size = original_img.size  # (width, height)

        # Find bounding box of non-transparent area
        bbox = img_no_bg.getbbox()
        if bbox:
            cropped = img_no_bg.crop(bbox)
        else:
            print("[WARN] No non-transparent pixels found. Using original.")
            cropped = img_no_bg

        # Create a transparent canvas with original size
        canvas = Image.new("RGBA", original_size, (0, 0, 0, 0))

        # Calculate position to center
        paste_x = (original_size[0] - cropped.width) // 2
        paste_y = (original_size[1] - cropped.height) // 2

        # Paste cropped object onto canvas
        canvas.paste(cropped, (paste_x, paste_y), cropped)

        print(f"[INFO] Saving result to: {output_path}")
        canvas.save(output_path, "PNG")

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

    exit_code = remove_background_and_center(input_path, output_path)
    sys.exit(exit_code)
