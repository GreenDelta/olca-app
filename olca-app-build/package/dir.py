from dataclasses import dataclass
from pathlib import Path
import shutil

from package import BLAS_JNI_VERSION, MKL_JNI_VERSION, PROJECT_DIR
from package.dist import OsArch


@dataclass
class BuildDir:
    osa: OsArch

    @property
    def name(self):
        if self.osa == OsArch.LINUX_X64:
            return "linux.gtk.x86_64"
        if self.osa == OsArch.WINDOWS_X64:
            return "win32.win32.x86_64"
        if self.osa == OsArch.MACOS_X64:
            return "macosx.cocoa.x86_64"
        if self.osa == OsArch.MACOS_ARM:
            return "macosx.cocoa.aarch64"
        raise AssertionError(f"Unknown build name {self.osa}")

    @property
    def root(self) -> Path:
        return PROJECT_DIR / "build" / "temp" / self.name

    @property
    def export_dir(self) -> Path:
        return PROJECT_DIR / "build" / self.name

    @property
    def app(self) -> Path:
        if self.osa.is_mac():
            return self.root / "openLCA/openLCA.app/Contents/Eclipse"
        else:
            return self.root / "openLCA"

    @property
    def about(self) -> Path:
        return self.app

    @property
    def jre(self) -> Path:
        return self.app / "jre"

    @property
    def olca_plugin(self) -> Path | None:
        plugin_dir = self.app / "plugins"
        if not plugin_dir.exists() or not plugin_dir.is_dir():
            print(f"Warning: could not locate plugin folder: {plugin_dir}.")
            return None
        for p in plugin_dir.iterdir():
            if p.name.startswith("olca-app") and p.is_dir():
                return p
        print(f"Warning: olca-app plugin folder not found in: {plugin_dir}.")
        return None

    @property
    def blas_lib(self) -> Path:
        arch = "arm64" if self.osa == OsArch.MACOS_ARM else "x64"
        return self.app / f"olca-native/{BLAS_JNI_VERSION}/{arch}"

    @property
    def mkl_lib(self) -> Path:
        arch = "arm64" if self.osa == OsArch.MACOS_ARM else "x64"
        return self.app / f"olca-mkl-{arch}_v{MKL_JNI_VERSION}"

    def copy_export(self):
        if not self.export_dir.exists():
            print(f"No export available for copy the {self.osa.value} version.")
            return
        delete(self.root)
        self.root.parent.mkdir(exist_ok=True, parents=False)
        shutil.copytree(self.export_dir, self.root)

    def unjar_plugins(self):
        """Sometimes in newer Eclipse versions the PDE build does not respect
        the `Eclipse-BundleShape: dir` entry in plugin manifests anymore but
        exports them as jar-files. For plugins that should be extracted as
        folders, we check if there is a jar-file and extract it if this is
        the case."""

        plugin_dir = self.app / "plugins"
        jars = [
            "org.eclipse.equinox.launcher.*.jar",
            "olca-app_*.jar",
            "org.eclipse.ui.themes_*.jar",
        ]
        for jar in jars:
            for g in plugin_dir.glob(jar):
                name = g.name[0:len(g.name) - 4]
                print(f"info: unpack plugin {name}")
                shutil.unpack_archive(g, plugin_dir / name, "zip")
                g.unlink()


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
            print("Cleaning dist folder...")
            shutil.rmtree(d, ignore_errors=True)
        d.mkdir(parents=True, exist_ok=True)
