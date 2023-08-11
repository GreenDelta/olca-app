import os
import urllib.request

from pathlib import Path

from package import PROJECT_DIR
from package.dir import BuildDir, delete
from package.dist import OsArch
from package.zipio import Zip


class JRE:

    # the bundle ID of the JRE
    ID = "org.openlca.jre"

    @staticmethod
    def zip_name(osa: OsArch) -> str:
        suffix = "zip" if osa == OsArch.WINDOWS_X64 else "tar.gz"
        if osa == OsArch.MACOS_ARM:
            name = "aarch64_mac"
        elif osa == OsArch.MACOS_X64:
            name = "x64_mac"
        elif osa == OsArch.LINUX_X64:
            name = "x64_linux"
        elif osa == OsArch.WINDOWS_X64:
            name = "x64_windows"
        else:
            raise ValueError(f"Warning: Unsupported OS + arch: {osa}.")
        return f"OpenJDK17U-jre_{name}_hotspot_17.0.5_8.{suffix}"

    @staticmethod
    def cache_dir() -> Path:
        d = PROJECT_DIR / "runtime/jre"
        if not os.path.exists(d):
            d.mkdir(parents=True, exist_ok=True)
        return d

    @staticmethod
    def fetch(osa: OsArch) -> Path:
        zip_name = JRE.zip_name(osa)
        cache_dir = JRE.cache_dir()
        zf = cache_dir / JRE.zip_name(osa)
        if os.path.exists(zf):
            return zf
        url = (
            "https://github.com/adoptium/temurin17-binaries/releases/"
            f"download/jdk-17.0.5%2B8/{zip_name}"
        )
        print(f"  Fetching JRE from {url}...")
        urllib.request.urlretrieve(url, zf)
        if not os.path.exists(zf):
            raise AssertionError(f"Warning: JRE download failed; url={url}")
        return zf

    @staticmethod
    def extract_to(build_dir: BuildDir):
        if build_dir.jre.exists():
            return
        print("  Copying JRE...")

        # fetch and extract the JRE
        zf = JRE.fetch(build_dir.osa)

        ziptool = Zip.get()
        if not ziptool.is_z7 or zf.name.endswith(".zip"):
            Zip.unzip(zf, build_dir.app)
        else:
            tar = zf.parent / zf.name[0:-3]
            if not tar.exists():
                Zip.unzip(zf, zf.parent)
                if not tar.exists():
                    raise AssertionError(f"Warning: could not find the JRE tar {tar}.")
            Zip.unzip(tar, build_dir.app)

        # rename the JRE folder if required
        if build_dir.jre.exists():
            return
        jre_dir = next(build_dir.app.glob("*jre*"))
        os.rename(jre_dir, build_dir.jre)

        # delete a possible client VM (the server VM is much faster)
        client_dir = build_dir.jre / "bin/client"
        delete(client_dir)
