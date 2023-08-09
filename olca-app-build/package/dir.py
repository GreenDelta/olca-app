from dataclasses import dataclass
from pathlib import Path
import shutil

from package import BLAS_JNI_VERSION, PROJECT_DIR
from package.dist import OsArch


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
    def app(self) -> Path:
        if self.osa.is_mac():
            return self.root / "openLCA/openLCA.app"
        else:
            return self.root / "openLCA"

    @property
    def about(self) -> Path:
        if self.osa.is_mac():
            return self.app / "Contents/Eclipse"
        else:
            return self.app

    @property
    def jre(self) -> Path:
        if self.osa.is_mac():
            return self.root / "openLCA/openLCA.app/Contents/Eclipse/jre"
        else:
            return self.app / "jre"

    @property
    def olca_plugin(self) -> Path | None:
        if self.osa.is_mac():
            plugin_dir = self.app / "Contents/Eclipse/plugins"
        else:
            plugin_dir = self.app / "plugins"
        if not plugin_dir.exists() or not plugin_dir.is_dir():
            print(f"warning: could not locate plugin folder: {plugin_dir}")
            return None
        for p in plugin_dir.iterdir():
            if p.name.startswith("olca-app") and p.is_dir():
                return p
        print(f"warning: olca-app plugin folder not found in: {plugin_dir}")
        return None

    @property
    def native_lib(self) -> Path:
        arch = "arm64" if self.osa == OsArch.MACOS_ARM else "x64"
        if self.osa.is_mac():
            target_dir = self.app / "Contents/Eclipse"
        else:
            target_dir = self.app
        return target_dir / f"olca-native/{BLAS_JNI_VERSION}/{arch}"


def delete(path: Path):
    if not path.exists():
        return
    if path.is_dir():
        shutil.rmtree(path, ignore_errors=True)
    else:
        path.unlink(missing_ok=True)


class DistDir:
    @staticmethod
    def get() -> Path:
        d = PROJECT_DIR / "build/dist"
        if not d.exists():
            d.mkdir(parents=True, exist_ok=True)
        return d

    @staticmethod
    def clean():
        d = DistDir.get()
        if d.exists():
            print("clean dist folder")
            shutil.rmtree(d, ignore_errors=True)
        d.mkdir(parents=True, exist_ok=True)
