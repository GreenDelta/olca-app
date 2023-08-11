import os
import urllib.request
import zipfile

from pathlib import Path

from package.dir import BuildDir
from package.dist import Lib, OsArch
from package.mkl import MKLFramework


class NativeLib:

    GITHUB = "Github"
    MAVEN = "Maven"


    @staticmethod
    def base_name(osa: OsArch) -> str:
        if osa == OsArch.MACOS_ARM:
            arch = "macos-arm64"
        elif osa == OsArch.MACOS_X64:
            arch = "macos-x64"
        elif osa == OsArch.LINUX_X64:
            arch = "linux-x64"
        elif osa == OsArch.WINDOWS_X64:
            arch = "win-x64"
        else:
            raise ValueError(f"Warning: Unsupported OS + arch: {osa}.")
        return f"olca-native-blas-{arch}"
    
    @staticmethod
    def fetch(osa: OsArch, lib: Lib, platform: str) -> Path:
        base_name = lib.base_name(osa)
        version = lib.version()

        if platform == NativeLib.GITHUB:
            jar = f"{base_name}.zip"
        else:
            jar = f"{base_name}-{version}.jar"

        cached = lib.cache_dir() / jar
        if cached.exists():
            return cached
        print(f"  Fetching {lib.name} native lib from {platform} repository...")

        if platform == NativeLib.GITHUB:
            repo = lib.github_repo()
            url = (
                f"https://github.com/GreenDelta/{repo}/releases/download/"
                f"v{version}/{jar}"
            )
        elif lib == Lib.BLAS:
            url = (
                f"https://repo1.maven.org/maven2/org/openlca/"
                f"{base_name}/{lib.version()}/{jar}"
            )
        else:
            raise AssertionError(f"There is no MKL native library on Maven.")

        print(f"  Fetching {lib.name} native libraries from {url}...")
        urllib.request.urlretrieve(url, cached)
        if not os.path.exists(cached):
            raise AssertionError(f"Warning: the native library download failed; url={url}")
        
        return cached

    @staticmethod
    def extract_to(build_dir: BuildDir, lib: Lib, platform: str = GITHUB):
        print("  Copying native libraries...")
        target = build_dir.blas_lib if lib == Lib.BLAS else build_dir.mkl_lib
        if not target.exists():
            target.mkdir(parents=True, exist_ok=True)

        if lib == Lib.MKL:
            MKLFramework.extract_to(target, build_dir.osa)

        jar = NativeLib.fetch(build_dir.osa, lib, platform)

        with zipfile.ZipFile(jar.as_posix(), "r") as z:
            for e in z.filelist:
                if e.is_dir():
                    continue
                name = Path(e.filename).name
                if name.endswith((".MF", ".xml", ".properties")):
                    continue
                target_file = target / name
                target_file.write_bytes(z.read(e))
