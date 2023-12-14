import os
import platform
import shutil
import subprocess
import sys
import urllib.request

from pathlib import Path
from typing import Optional

from package import PROJECT_DIR


class Zip:
    __zip: Optional["Zip"] = None

    def __init__(self, is_z7: bool):
        self.is_z7 = is_z7

    @staticmethod
    def z7() -> Path:
        return PROJECT_DIR / "tools/7zip/7za.exe"

    @staticmethod
    def get() -> "Zip":
        if Zip.__zip is not None:
            return Zip.__zip
        system = platform.system().lower()
        if system != "windows":
            Zip.__zip = Zip(False)
            return Zip.__zip
        z7 = Zip.z7()
        if os.path.exists(z7):
            Zip.__zip = Zip(True)
            return Zip.__zip

        # try to fetch a version 7zip version from the web
        url = "https://www.7-zip.org/a/7za920.zip"
        print(
            f"Warning: no 7zip version found under {z7}, will download an OLD"
            f" version from {url}"
        )
        z7_dir = PROJECT_DIR / "tools/7zip"
        z7_dir.mkdir(parents=True, exist_ok=True)
        z7_zip = z7_dir / "7zip.zip"
        urllib.request.urlretrieve(url, z7_zip)
        shutil.unpack_archive(z7_zip, z7_dir)
        Zip.__zip = Zip(os.path.exists(z7))
        return Zip.__zip

    @staticmethod
    def unzip(zip_file: Path, target_folder: Path):
        """Extracts the content of the given zip file under the given path."""
        if not target_folder.exists():
            target_folder.mkdir(parents=True, exist_ok=True)
        if Zip.get().is_z7:
            Zip.run_quietly([Zip.z7(), "x", zip_file, f"-o{target_folder}"])
        else:
            shutil.unpack_archive(zip_file, target_folder)

    @staticmethod
    def targz(folder: Path, target: Path):
        if not target.parent.exists():
            target.parent.mkdir(parents=True, exist_ok=True)

        # remove possible extensions from the given target file
        base_name = target.name
        if base_name.endswith(".tar.gz"):
            base_name = base_name[0:-7]
        elif base_name.endswith(".tar"):
            base_name = base_name[0:-4]
        base = target.parent / base_name

        # package the folder
        if Zip.get().is_z7:
            tar = target.parent / (base_name + ".tar")
            gz = target.parent / (base_name + ".tar.gz")
            Zip.run_quietly(
                [Zip.z7(), "a", "-ttar", str(tar), folder.as_posix() + "/*"]
            )
            Zip.run_quietly([Zip.z7(), "a", "-tgzip", str(gz), str(tar)])
            os.remove(tar)
        else:
            shutil.make_archive(str(base), "gztar", str(folder))
    
    @staticmethod
    def run_quietly(args: list[str | Path]):
        process = subprocess.Popen(args=args,
                                   stdout=subprocess.PIPE,
                                   universal_newlines=True)
        if process.stdout is None:
            return
        # quietly printing the logs (remove the "Extracting  <file>" lines)
        for line in process.stdout:
            if "ing  " not in line:
                sys.stdout.write(f"    {line}")
