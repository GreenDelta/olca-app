import os
import datetime
import urllib.request

from enum import Enum
from dataclasses import dataclass
from pathlib import Path

# the root of the build project olca-app/olca-app-build
PROJECT_DIR = Path(os.path.dirname(os.path.abspath(__file__)))


class OsArch(Enum):
    MACOS_ARM = "macos-arm64"
    MACOS_X64 = "macos-x64"
    WINDOWS_X64 = "win-x64"
    LINUX_X64 = "linux-x64"


@dataclass
class Version:
    app_version: str

    @staticmethod
    def get() -> 'Version':
        # read app version from the app-manifest
        manifest = PROJECT_DIR.parent / Path("olca-app/META-INF/MANIFEST.MF")
        print(f'read version from {manifest}')
        app_version = None
        with open(manifest, 'r', encoding='utf-8') as f:
            for line in f:
                text = line.strip()
                if not text.startswith('Bundle-Version'):
                    continue
                app_version = text.split(':')[1].strip()
                break
        if app_version is None:
            app_version = '2.0.0'
            print(f'WARNING failed to read version from {manifest},'
                  f' default to {app_version}')
        return Version(app_version)

    @property
    def app_suffix(self):
        return f'{self.app_version}_{datetime.date.today().isoformat()}'


class JRE:

    @staticmethod
    def zip_name(osa: OsArch) -> str:
        suffix = 'zip' if osa == OsArch.WINDOWS_X64 else 'tar.gz'
        if osa == OsArch.MACOS_ARM:
            name = 'aarch64_mac'
        elif osa == OsArch.MACOS_X64:
            name = 'x64_mac'
        elif osa == OsArch.LINUX_X64:
            name = 'x64_linux'
        elif osa == OsArch.WINDOWS_X64:
            name = 'x64_windows'
        else:
            raise ValueError(f'unsupported OS+arch: {osa}')
        return f'OpenJDK17U-jre_{name}_hotspot_17.0.2_8.{suffix}'

    @staticmethod
    def cache_dir() -> Path:
        return PROJECT_DIR / 'runtime/jre'

    @staticmethod
    def fetch(osa: OsArch) -> Path:
        zip_name = JRE.zip_name(osa)
        cache_dir = JRE.cache_dir()
        zf = cache_dir / JRE.zip_name(osa)
        if os.path.exists(zf):
            return zf
        if not os.path.exists(cache_dir):
            cache_dir.mkdir()
        url = 'https://github.com/adoptium/temurin17-binaries/releases/' \
              f'download/jdk-17.0.2%2B8/{zip_name}'
        with urllib.request.urlopen(url) as inp:
            with open(zf, 'wb') as out:
                out.write(inp)
        if not os.path.exists(zf):
            raise AssertionError(f'JRE download failed; url={url}')
        return zf


if __name__ == '__main__':
    JRE.cache_dir().mkdir(parents=True, exist_ok=True)
    # JRE.fetch(OsArch.WINDOWS_X64)
