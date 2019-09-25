import os
import shutil
from pathlib import Path

if __name__ == '__main__':
    src = "dist"
    dest = os.path.join(str(Path.home()),
                        'openLCA-data-1.4', 'html', 'olca-app')
    shutil.rmtree(dest, ignore_errors=True)
    shutil.copytree(src, dest)
