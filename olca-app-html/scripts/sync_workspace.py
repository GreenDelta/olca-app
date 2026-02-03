# Updates the HTML pages in the openLCA workspace folder using the
# build output from the dist folder.

import shutil
from pathlib import Path

if __name__ == "__main__":
    dist = Path(__file__).parent.parent / "dist"
    dest = Path.home() / "openLCA-data-1.4" / "html" / "olca-app"
    if dest.exists():
        print(f"delete old workspace folder: ${dest}")
        shutil.rmtree(dest, ignore_errors=True)
    print(f"copy dist to ${dest}")
    shutil.copytree(dist, dest)
    print("done")
