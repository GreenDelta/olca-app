import argparse
import glob
import json
import os
import platform
import requests
import shutil
import sys
import zipfile

from enum import Enum
from pathlib import Path


BUILD_ROOT = Path(os.path.dirname(os.path.abspath(__file__)))

# BLAS libs
BLAS_DIR = Path("runtime/blas")
VERSION = "0.0.1"
MAVEN_REPO = "https://repo1.maven.org/maven2/org/openlca/"
PACKAGE_PREFIXE = "olca-native-blas-"
PACKAGE_PATH = "org/openlca/nativelib/"
INDEX_JSON = "olca-native.json"

# JRE
JRE_DIR = Path("runtime/jre")
URL_ADOPTIUM = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/"


class OS(Enum):
    MACOS_ARM = "macos-arm64"
    MACOS_X64 = "macos-x64"
    WINDOWS = "win-x64"
    LINUX = "linux-x64"

    def short(self):
        if self == OS.MACOS_ARM:
            return "macos-arm"
        elif self == OS.MACOS_X64:
            return "macos-x64"
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

    def get_jre_name(self):
        if self == OS.LINUX:
            return "OpenJDK17U-jre_x64_linux_hotspot_17.0.2_8.tar.gz"
        if self == OS.MACOS_X64:
            return "OpenJDK17U-jre_x64_mac_hotspot_17.0.2_8.tar.gz"
        if self == OS.WINDOWS:
            return "OpenJDK17U-jre_x64_windows_hotspot_17.0.2_8.zip"
        if self == OS.MACOS_ARM:
            sys.exit("No JRE for this os: " + self.value)
        sys.exit("unknown os: " + self.value)

    @classmethod
    def from_str(cls, _os: str) -> 'OS':
        if _os == OS.MACOS_ARM.short():
            return OS.MACOS_ARM
        if _os == OS.MACOS_X64.short():
            return OS.MACOS_X64
        if _os == OS.WINDOWS.short():
            return OS.WINDOWS
        if _os == OS.LINUX.short():
            return OS.LINUX

    @classmethod
    def get_os(cls) -> 'OS':
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


def check_libs(_os: OS) -> bool:
    directory = BUILD_ROOT / BLAS_DIR / _os.short()
    if not os.path.isdir(directory):
        print(f"Creating the directory: {directory}")
        Path(directory).mkdir(parents=True, exist_ok=True)
        return False
    paths_list = directory.glob('**/*')
    files = [path.name for path in paths_list if path.is_file()]
    if INDEX_JSON in files:
        print(f"{_os.short()} BLAS library are located in {directory}")
        return True
    else:
        print(f"{_os.short()} BLAS library could not be found in {directory}")
        return False


def fetch_libs(_os: OS):
    if check_libs(_os):
        return

    # Downloading the archive.
    print(f"Downloading {_os.short()} .jar.")
    package_name = PACKAGE_PREFIXE + _os.value
    archive_name = package_name + "-" + VERSION + ".jar"
    archive_path = BUILD_ROOT / BLAS_DIR / _os.short() / archive_name
    url = MAVEN_REPO + package_name + "/" + VERSION + "/" + archive_name
    try:
        r = requests.get(url, allow_redirects=True)
    except OSError as e:
        print(f"Failed to download {url} due to: \n{e}")
        return

    if not r.status_code == 200:
        print(f"<Error {r.status_code}> Failed to download {url}.")
        return

    open(archive_path, 'wb').write(r.content)

    # Extracting the files.
    with zipfile.ZipFile(archive_path, 'r') as archive:

        # Extracting the index file.
        directory = BUILD_ROOT / BLAS_DIR / _os.short()
        target_file = directory / INDEX_JSON
        file_path = PACKAGE_PATH + INDEX_JSON
        with open(target_file, 'wb') as f:
            print(f"Extraction {file_path}")
            f.write(archive.read(str(file_path)))
        with open(target_file) as json_file:
            index = json.load(json_file)

        if index["modules"] != ["blas"]:
            print("The fetched archive contains other modules than BLAS.")
            print("Removing the index file.")
            os.remove(target_file)
        else:
            # Extracting the libraries.
            for lib in index["libraries"]:
                file_path = PACKAGE_PATH + lib
                target_file = directory / lib
                print(file_path)
                with open(target_file, 'wb') as f:
                    print(f"Extraction {file_path}")
                    f.write(archive.read(str(file_path)))

    print("Removing the .jar file.")
    os.remove(archive_path)
    return


def clean_libs(_os: OS):
    directory = BUILD_ROOT / BLAS_DIR / _os.short()
    print(f"Removing {directory}")
    shutil.rmtree(directory, ignore_errors=True)
    return


def check_jre(_os: OS) -> bool:
    directory = BUILD_ROOT / JRE_DIR / _os.short()
    if not os.path.isdir(directory):
        print(f"Creating the directory: {directory}")
        Path(directory).mkdir(parents=True, exist_ok=True)
        return False
    if glob.glob(str(directory / _os.get_jre_name())):
        print(f"{_os.short()} JRE is located in {directory}")
        return True
    else:
        print(f"{_os.short()} JRE could not be found in {directory}")
        return False


def fetch_jre(_os: OS):
    if check_jre(_os):
        return

    # Downloading the JRE.
    print(f"Downloading {_os.short()} JRE.")
    archive_name = _os.get_jre_name()
    archive_path = BUILD_ROOT / JRE_DIR / _os.short() / archive_name
    url = f'{URL_ADOPTIUM}/{archive_name}'
    try:
        r = requests.get(url, allow_redirects=True)
    except OSError as e:
        print(f"Failed to download {url} due to: \n{e}")
        return

    if not r.status_code == 200:
        print(f"<Error {r.status_code}> Failed to download {url}.")
        return

    open(archive_path, 'wb').write(r.content)


def clean_jre(_os: OS):
    archive_name = _os.get_jre_name()
    archive_path = BUILD_ROOT / JRE_DIR / _os.short() / archive_name
    print(f"Removing {archive_path}")
    shutil.rmtree(archive_path)
    return


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument("action", choices=["fetch", "clean", "check"])

    parser.add_argument("--os", "-o", type=str, choices=[_os.short() for _os in OS],
                        help="Select the OS for which the script is suppose to run.")
    parser.add_argument("--element", "-e", type=str, choices=["libs", "jre"], help="Select the element to be fetched.")

    # Read arguments from the command line
    args = parser.parse_args()

    print(args)

    if args.action == "fetch":
        function_libs = fetch_libs
        function_jre = fetch_jre
    elif args.action == "check":
        function_libs = check_libs
        function_jre = check_jre
    elif args.action == "clean":
        function_libs = clean_libs
        function_jre = clean_jre
    else:
        return

    if args.element is None:
        if args.os is None:
            for _os in OS:
                function_libs(_os)
                function_jre(_os)
        else:
            function_libs(OS.from_str(args.os))
            function_jre(OS.from_str(args.os))
    elif args.element == "libs":
        if args.os is None:
            for _os in OS:
                function_libs(_os)
        else:
            function_libs(OS.from_str(args.os))
    elif args.element == "jre":
        if args.os is None:
            for _os in OS:
                function_jre(_os)
        else:
            function_jre(OS.from_str(args.os))
    return


if __name__ == '__main__':
    main()
