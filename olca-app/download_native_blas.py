import os
import platform
import zipfile

import requests
import sys

from enum import Enum
from pathlib import Path


MODULE_ROOT = Path(os.path.dirname(os.path.abspath(__file__)))
PROJECT_ROOT = MODULE_ROOT.parent
BLAS_DIR = Path("olca-app-build/runtime/blas")
VERSION = "0.0.1"
MAVEN_REPO = "https://repo1.maven.org/maven2/org/openlca/"


class OS(Enum):
    MACOS_ARM = "macos-arm64"
    MACOS_X64 = "macos-x64"
    WINDOWS = "win-x64"
    LINUX = "linux-x64"

    def short(self):
        if self == OS.MACOS_ARM or self == OS.MACOS_X64:
            return "macos"
        if self == OS.WINDOWS:
            return "win"
        if self == OS.LINUX:
            return "linux"

    def get_lib_ext(self) -> str:
        if self == OS.LINUX:
            return ".so"
        if self == OS.MACOS_X64 or self == OS.MACOS_ARM:
            return ".dylib"
        if self == OS.WINDOWS:
            return ".dll"
        sys.exit("unknown os: " + self.value)

    def get_identification_file_name(self) -> str:
        if self == OS.LINUX:
            return "libolcar"
        else:
            return "olcar"


def get_os() -> OS:
    platform_system = platform.system().lower()
    if platform_system == "windows":
        return OS.WINDOWS
    platform_platform = platform.platform()
    if platform_system == "linux":
        return OS.LINUX
    if platform_system == "darwin":
        if "arm" in platform_platform:
            return OS.MACOS_ARM
        if "x64" in platform_platform:
            return OS.MACOS_X64

    sys.exit("unknown platform: " + platform_system)


def check(_os: OS) -> bool:
    directory = PROJECT_ROOT / BLAS_DIR / _os.short()
    if not os.path.isdir(directory):
        sys.exit(f"{directory} does not exist")
    paths_list = directory.glob('**/*')
    # olcar lib file has only one extension, so no need to complicate things.
    files = [path.stem for path in paths_list if path.is_file()]
    if "olcar" in files:
        print(f"{_os.short()} BLAS library are located in {directory}")
        return True
    else:
        print(f"{_os.short()} BLAS library could not be found in {directory}")
        return False


def download(_os: OS):
    if check(_os):
        return

    # Downloading the archive.
    print(f"Downloading {_os} .jar.")
    package_name = "olca-native-blas-" + _os.value
    archive_name = package_name + "-" + VERSION + ".jar"
    archive_path = PROJECT_ROOT / BLAS_DIR / _os.short() / archive_name
    url = MAVEN_REPO + package_name + "/" + VERSION + "/" + archive_name
    r = requests.get(url, allow_redirects=True)
    if not r.status_code == 200:
        sys.exit(f"<Error {r.status_code}> Failed to download {url}.")

    open(archive_path, 'wb').write(r.content)

    # Extracting the files.
    with zipfile.ZipFile(archive_path, 'r') as _archive:
        info_list = _archive.infolist()
        directory = PROJECT_ROOT / BLAS_DIR / _os.short()
        for info in info_list:
            file_path = Path(info.filename)
            # For Linux lib, (".so" in file_name) is enough to filter .so.{int}.
            if _os.get_lib_ext() in file_path.name or \
                    file_path.name == "olca-native.json":
                _file_path = directory / file_path.name
                with open(_file_path, 'wb') as f:
                    print(f"Extraction {file_path.name}")
                    f.write(_archive.read(str(file_path)))

    print("Removing the .jar file.")
    os.remove(archive_path)


def download_all():
    print("Downloading every OS libraries.")
    for _os in OS:
        download(_os)


def main():
    args = sys.argv
    if len(args) < 2:
        return
    cmd = args[1]
    if cmd == "download":
        if len(args) < 3:
            download_all()
        _os = args[2]
        if _os == "macos":
            download(OS.MACOS_X64)
        elif _os == "linux":
            download(OS.LINUX)
        elif _os == "win":
            download(OS.WINDOWS)


if __name__ == '__main__':
    main()
