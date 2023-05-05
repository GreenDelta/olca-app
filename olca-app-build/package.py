import datetime
import os
import platform
import re
import shutil
import subprocess
import sys
import urllib.request
import xml.etree.ElementTree as ElementTree
import zipfile

from enum import Enum
from dataclasses import dataclass
from pathlib import Path
from typing import cast, Optional

# the version of the native library package
NATIVE_LIB_VERSION = "0.0.1"

# the root of the build project olca-app/olca-app-build
PROJECT_DIR = Path(os.path.dirname(os.path.abspath(__file__)))


class OsArch(Enum):
    MACOS_ARM = "macOS_arm64"
    MACOS_X64 = "macOS_x64"
    WINDOWS_X64 = "Windows_x64"
    LINUX_X64 = "Linux_x64"

    def is_mac(self) -> bool:
        return self == OsArch.MACOS_ARM or self == OsArch.MACOS_X64

    def is_win(self) -> bool:
        return self == OsArch.WINDOWS_X64

    def is_linux(self) -> bool:
        return self == OsArch.LINUX_X64


@dataclass
class Version:
    app_version: str

    @staticmethod
    def get() -> "Version":
        # read app version from the app-manifest
        manifest = PROJECT_DIR.parent / Path("olca-app/META-INF/MANIFEST.MF")
        print(f"read version from {manifest}")
        app_version = None
        with open(manifest, "r", encoding="utf-8") as f:
            for line in f:
                text = line.strip()
                if not text.startswith("Bundle-Version"):
                    continue
                app_version = text.split(":")[1].strip()
                break
        if app_version is None:
            app_version = "2.0.0"
            print(
                f"WARNING failed to read version from {manifest},"
                f" default to {app_version}"
            )
        return Version(app_version)

    @property
    def app_suffix(self):
        return f"{self.app_version}_{datetime.date.today().isoformat()}"

    @property
    def base(self) -> str:
        m = re.search(r"(\d+(\.\d+)?(\.\d+)?)", self.app_version)
        return "2" if m is None else m.group(0)


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
            f"WARNING no 7zip version found under {z7}, will download an OLD"
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
            subprocess.call([Zip.z7(), "x", zip_file, f"-o{target_folder}"])
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
            tar = base.with_suffix(".tar")
            gz = base.with_suffix(".tar.gz")
            subprocess.call(
                [Zip.z7(), "a", "-ttar", str(tar), folder.as_posix() + "/*"]
            )
            subprocess.call([Zip.z7(), "a", "-tgzip", str(gz), str(tar)])
            os.remove(tar)
        else:
            shutil.make_archive(str(base), "gztar", str(folder))


class Build:
    @staticmethod
    def dist_dir() -> Path:
        d = PROJECT_DIR / "build/dist"
        if not d.exists():
            d.mkdir(parents=True, exist_ok=True)
        return d

    @staticmethod
    def clean():
        d = Build.dist_dir()
        if d.exists():
            print("clean dist folder")
            shutil.rmtree(d, ignore_errors=True)
        d.mkdir(parents=True, exist_ok=True)


@dataclass
class BuildDir:
    osa: OsArch

    @property
    def root(self) -> Path:
        build_dir = PROJECT_DIR / "build"
        if self.osa == OsArch.LINUX_X64:
            return build_dir / "linux.gtk.x86_64"
        if self.osa == OsArch.WINDOWS_X64:
            return build_dir / "win32.win32.x86_64"
        if self.osa == OsArch.MACOS_X64:
            return build_dir / "macosx.cocoa.x86_64"
        if self.osa == OsArch.MACOS_ARM:
            return build_dir / "macosx.cocoa.aarch64"
        raise AssertionError(f"unknown build target {self.osa}")

    @property
    def exists(self) -> bool:
        return self.root.exists()

    @property
    def app_dir(self) -> Path:
        if self.osa.is_mac():
            return self.root / "openLCA/openLCA.app"
        else:
            return self.root / "openLCA"

    @property
    def readme_dir(self) -> Path:
        if self.osa.is_mac():
            return self.app_dir / "Contents/Eclipse"
        else:
            return self.app_dir

    @property
    def jre_dir(self) -> Path:
        if self.osa.is_mac():
            return self.root / "openLCA/openLCA.app/Contents/Eclipse/jre"
        else:
            return self.app_dir / "jre"

    @property
    def olca_plugin_dir(self) -> Path | None:
        if self.osa.is_mac():
            plugin_dir = self.app_dir / "Contents/Eclipse/plugins"
        else:
            plugin_dir = self.app_dir / "plugins"
        if not plugin_dir.exists() or not plugin_dir.is_dir():
            print(f"warning: could not locate plugin folder: {plugin_dir}")
            return None
        for p in plugin_dir.iterdir():
            if p.name.startswith("olca-app") and p.is_dir():
                return p
        print(f"warning: olca-app plugin folder not found in: {plugin_dir}")
        return None

    @property
    def native_lib_dir(self) -> Path:
        arch = "arm64" if self.osa == OsArch.MACOS_ARM else "x64"
        if self.osa.is_mac():
            target_dir = self.app_dir / "Contents/Eclipse"
        else:
            target_dir = self.app_dir
        return target_dir / f"olca-native/{NATIVE_LIB_VERSION}/{arch}"

    def package(self, version: Version):

        if self.osa.is_mac():
            MacDir.arrange(self)

        # JRE and native libraries
        JRE.extract_to(self)
        NativeLib.extract_to(self, repo=NativeLib.REPO_GITHUB)

        # copy credits
        print("  copy credits")
        about_page = PROJECT_DIR / "credits/about.html"
        if about_page.exists():
            shutil.copy2(about_page, self.readme_dir)
            plugin_dir = self.olca_plugin_dir
            if plugin_dir:
                shutil.copy2(about_page, plugin_dir)

        # copy ini and bin files
        bins: list[str] = []
        if self.osa.is_win():
            Template.apply(
                PROJECT_DIR / "templates/openLCA_win.ini",
                self.app_dir / "openLCA.ini",
                encoding="iso-8859-1",
                lang="en",
            )
            bins = ["ipc-server.cmd", "grpc-server.cmd"]
        if self.osa.is_linux():
            shutil.copy2(
                PROJECT_DIR / "templates/openLCA_linux.ini",
                self.app_dir / "openLCA.ini",
            )
            bins = ["ipc-server.sh", "grpc-server.sh"]
        if len(bins) > 0:
            bin_dir = self.app_dir / "bin"
            bin_dir.mkdir(exist_ok=True, parents=True)
            for binary in bins:
                bin_source = PROJECT_DIR / f"bin/{binary}"
                bin_target = bin_dir / binary
                if not bin_source.exists() or bin_target.exists():
                    continue
                shutil.copy2(bin_source, bin_target)

        # build the package
        pack_name = f"openLCA_{self.osa.value}_{version.app_suffix}"
        print(f"  create package {pack_name}")
        pack = Build.dist_dir() / pack_name
        if self.osa == OsArch.WINDOWS_X64:
            shutil.make_archive(pack.as_posix(), "zip", self.root.as_posix())
        else:
            Zip.targz(self.root, pack)

        if self.osa.is_win():
            Nsis.run(self, version)


class JRE:
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
            raise ValueError(f"unsupported OS+arch: {osa}")
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
        print(f"  download JRE from {url}")
        urllib.request.urlretrieve(url, zf)
        if not os.path.exists(zf):
            raise AssertionError(f"JRE download failed; url={url}")
        return zf

    @staticmethod
    def extract_to(build_dir: BuildDir):
        if build_dir.jre_dir.exists():
            return
        print("  copy JRE")

        # fetch and extract the JRE
        zf = JRE.fetch(build_dir.osa)

        ziptool = Zip.get()
        if not ziptool.is_z7 or zf.name.endswith(".zip"):
            Zip.unzip(zf, build_dir.app_dir)
        else:
            tar = zf.parent / zf.name[0:-3]
            if not tar.exists():
                Zip.unzip(zf, zf.parent)
                if not tar.exists():
                    raise AssertionError(f"could not find JRE tar {tar}")
            Zip.unzip(tar, build_dir.app_dir)

        # rename the JRE folder if required
        if build_dir.jre_dir.exists():
            return
        jre_dir = next(build_dir.app_dir.glob("*jre*"))
        os.rename(jre_dir, build_dir.jre_dir)

        # delete a possible client VM (the server VM is much faster)
        client_dir = build_dir.jre_dir / "bin/client"
        if client_dir.exists():
            delete(client_dir)


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
            raise ValueError(f"unsupported OS+arch: {osa}")
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
            jar = f"{base_name}-{NATIVE_LIB_VERSION}.jar"

        cached = NativeLib.cache_dir() / jar
        if cached.exists():
            return cached
        print(f"  fetch native lib from {repo} repository")

        if repo == NativeLib.REPO_GITHUB:
            url = (
                "https://github.com/GreenDelta/olca-native/releases/"
                f"download/v{NATIVE_LIB_VERSION}/{jar}"
            )
        else:
            url = (
                f"https://repo1.maven.org/maven2/org/openlca/"
                f"{base_name}/{NATIVE_LIB_VERSION}/{jar}"
            )

        print(f"  download native libraries from {url}")
        urllib.request.urlretrieve(url, cached)
        if not os.path.exists(cached):
            raise AssertionError(f"native-library download failed; url={url}")
        return cached

    @staticmethod
    def extract_to(build_dir: BuildDir, repo: str = REPO_GITHUB):
        print("  copy native libraries")
        target = build_dir.native_lib_dir
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


class MacDir:
    @staticmethod
    def arrange(build_dir: BuildDir):

        # create the folder structure
        app_root = build_dir.root / "openLCA"
        app_dir = build_dir.app_dir
        eclipse_dir = app_dir / "Contents/Eclipse"
        macos_dir = app_dir / "Contents/MacOS"
        for d in (app_dir, eclipse_dir, macos_dir):
            d.mkdir(parents=True, exist_ok=True)

        # move files and folders
        moves = [
            (app_root / "configuration", eclipse_dir),
            (app_root / "plugins", eclipse_dir),
            (app_root / ".eclipseproduct", eclipse_dir),
            (app_root / "Resources", app_dir / "Contents"),
            (app_root / "MacOS/openLCA", macos_dir / "openLCA"),
        ]
        for (source, target) in moves:
            if source.exists():
                shutil.move(str(source), str(target))

        resources = PROJECT_DIR / "resources"
        shutil.copy2(resources / "openLCA.entitlements", app_dir / "Contents")
        MacDir.add_info_plist(app_dir / "Contents/Info.plist")

        # create the ini file
        plugins_dir = eclipse_dir / "plugins"
        launcher_jar = next(plugins_dir.glob("*launcher*.jar")).name
        launcher_lib = next(plugins_dir.glob("*launcher.cocoa.macosx*")).name
        Template.apply(
            PROJECT_DIR / "templates/openLCA_mac.ini",
            eclipse_dir / "openLCA.ini",
            launcher_jar=launcher_jar,
            launcher_lib=launcher_lib,
        )

        # clean up
        delete(app_root / "MacOS")
        delete(app_root / "Info.plist")
        delete(macos_dir / "openLCA.ini")

    @staticmethod
    def add_info_plist(path: Path):
        # set version of the app
        # (version must be composed of one to three period-separated integers.)
        info_dict = {
            "CFBundleShortVersionString": Version.get().base,
            "CFBundleVersion": Version.get().base,
        }
        info = ElementTree.parse(PROJECT_DIR / "templates/Info.plist")
        iterator = info.getroot().find("dict").iter()
        for elem in iterator:
            if elem.text in info_dict.keys():
                string = next(iterator, None)
                if string is not None:
                    string.text = info_dict[elem.text]

        with open(path, "wb") as out:
            out.write(
                b'<?xml version="1.0" encoding="UTF-8" standalone = '
                b'"no" ?>\n'
            )
            info.write(out, encoding="UTF-8", xml_declaration=False)


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
        print(f"  download NSIS from {url}")
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
            print("  skip NSIS installer build")
            return False
        if platform.system().lower() != "windows":
            print("WARNING: NSIS installers can be only build on Windows")

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
            dist_dir / f"openLCA_{build_dir.osa.name}"
            f"_{version.app_suffix}.exe"
        )
        shutil.move(build_dir.root / "setup.exe", app_file)


class Template:
    @staticmethod
    def apply(
        source: Path, target: Path, encoding: str = "utf-8", **kwargs: str
    ):
        with open(source, mode="r", encoding="utf-8") as inp:
            template = inp.read()
            text = template.format(**kwargs)
        with open(target, "w", encoding=encoding) as out:
            out.write(text)


def main():
    Build.clean()
    version = Version.get()
    for osa in OsArch:
        build_dir = BuildDir(osa)
        if not build_dir.exists:
            print(f"no {osa} build available; skipped")
            continue
        print(f"package build: {osa}")
        build_dir.package(version)


def delete(path: Path):
    if path is None or not path.exists():
        return
    if path.is_dir():
        shutil.rmtree(path, ignore_errors=True)
    else:
        path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
