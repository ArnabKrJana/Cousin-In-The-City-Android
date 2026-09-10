import os

PROJECT_ROOT = os.getcwd()   # 👈 current directory
OUTPUT_FILE = "all_android_code.txt"
EXTENSIONS = (".kt")

with open(OUTPUT_FILE, "w", encoding="utf-8") as out:
    for root, dirs, files in os.walk(PROJECT_ROOT):
        # skip noisy folders
        dirs[:] = [d for d in dirs if d not in ("build", ".gradle", ".idea")]

        for file in files:
            if file.endswith(EXTENSIONS):
                file_path = os.path.join(root, file)

                out.write("\n" + "=" * 100 + "\n")
                out.write(f"FILE: {os.path.relpath(file_path, PROJECT_ROOT)}\n")
                out.write("=" * 100 + "\n\n")

                try:
                    with open(file_path, "r", encoding="utf-8") as f:
                        out.write(f.read())
                except Exception as e:
                    out.write(f"[ERROR READING FILE]: {e}\n")

print(f"✅ Extracted into {OUTPUT_FILE}")
