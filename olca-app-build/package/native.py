import os
import urllib.request
import zipfile

from pathlib import Path

from package import BLAS_JNI_VERSION, PROJECT_DIR
from package.dir import BuildDir
from package.dist import OsArch


class NativeLib:

    REPO_GITHUB = "Github"
    REPO_MAVEN = "Maven"


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
    def cache_dir() -> Path:
        d = PROJECT_DIR / f"runtime/blas"
        if not os.path.exists(d):
            d.mkdir(parents=True, exist_ok=True)
        return d

    @staticmethod
    def fetch(osa: OsArch, repo: str) -> Path:

        base_name = NativeLib.base_name(osa)
        if repo == NativeLib.REPO_GITHUB:
            jar = f"{base_name}.zip"
        else:
            jar = f"{base_name}-{BLAS_JNI_VERSION}.jar"

        cached = NativeLib.cache_dir() / jar
        if cached.exists():
            return cached
        print(f"  Fetching native lib from {repo} repository...")

        if repo == NativeLib.REPO_GITHUB:
            url = (
                "https://github.com/GreenDelta/olca-native/releases/"
                f"download/v{BLAS_JNI_VERSION}/{jar}"
            )
        else:
            url = (
                f"https://repo1.maven.org/maven2/org/openlca/"
                f"{base_name}/{BLAS_JNI_VERSION}/{jar}"
            )

        print(f"  Fetching native libraries from {url}...")
        urllib.request.urlretrieve(url, cached)
        if not os.path.exists(cached):
            raise AssertionError(f"Warning: the native library download failed; url={url}")
        return cached

    @staticmethod
    def extract_to(build_dir: BuildDir, repo: str = REPO_GITHUB):
        print("  Copying native libraries...")
        target = build_dir.native_lib
        if not target.exists():
            target.mkdir(parents=True, exist_ok=True)

        jar = NativeLib.fetch(build_dir.osa, repo)

        with zipfile.ZipFile(jar.as_posix(), "r") as z:
            for e in z.filelist:
                if e.is_dir():
                    continue
                name = Path(e.filename).name
                if name.endswith((".MF", ".xml", ".properties")):
                    continue
                target_file = target / name
                target_file.write_bytes(z.read(e))
