import platform
import shutil
import subprocess
import sys
import urllib.request

from pathlib import Path

from package import PROJECT_DIR
from package.dir import BuildDir
from package.dist import Version
from package.template import Template
from package.zipio import Zip


class Nsis:
    VERSION = "2.51"

    @staticmethod
    def fetch() -> Path:
        nsis = PROJECT_DIR / f"tools/nsis-{Nsis.VERSION}/makensis.exe"
        if nsis.exists():
            return nsis
        url = (
            f"https://sourceforge.net/projects/nsis/files"
            f"/NSIS%202/{Nsis.VERSION}/nsis-{Nsis.VERSION}.zip/download"
        )
        print(f"  Fetching NSIS from {url}...")
        nsis_zip = PROJECT_DIR / f"tools/nsis-{Nsis.VERSION}.zip"
        urllib.request.urlretrieve(url, nsis_zip)
        Zip.unzip(nsis_zip, PROJECT_DIR / f"tools")
        if not nsis.exists():
            AssertionError(f"failed to fetch NSIS from {url}")
        return nsis

    @staticmethod
    def run(build_dir: BuildDir, version: Version):
        if not build_dir.osa.is_win():
            return
        if "--winstaller" not in sys.argv:
            print("  NSIS installer build is skipped.")
            return False
        if platform.system().lower() != "windows":
            print("Warning: NSIS installers can be only build on Windows")

        exe = Nsis.fetch()

        # installer resources
        inst_files = (PROJECT_DIR / "resources/installer_static_win").glob("*")
        for f in inst_files:
            shutil.copy2(f, build_dir.root / f.name)
        Template.apply(
            PROJECT_DIR / "templates/setup.nsi",
            build_dir.root / "setup.nsi",
            encoding="iso-8859-1",
            version=version.base,
        )

        # ini files with language flag
        en_dir = build_dir.root / "english"
        en_dir.mkdir(parents=True, exist_ok=True)
        Template.apply(
            PROJECT_DIR / "templates/openLCA_win.ini",
            en_dir / "openLCA.ini",
            encoding="iso-8859-1",
            lang="en",
        )
        de_dir = build_dir.root / "german"
        de_dir.mkdir(parents=True, exist_ok=True)
        Template.apply(
            PROJECT_DIR / "templates/openLCA_win.ini",
            de_dir / "openLCA.ini",
            encoding="iso-8859-1",
            lang="de",
        )

        # create the installer
        subprocess.call([exe, build_dir.root / "setup.nsi"])
        dist_dir = PROJECT_DIR / "build/dist"
        if not dist_dir.exists():
            dist_dir.mkdir(parents=True, exist_ok=True)
        app_file = (
            dist_dir / f"openLCA_{build_dir.osa.value}"
            f"_{version.app_suffix}.exe"
        )
        shutil.move(build_dir.root / "setup.exe", app_file)
