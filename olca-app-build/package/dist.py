import datetime
import re

from dataclasses import dataclass
from enum import Enum
from pathlib import Path

from package import PROJECT_DIR


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
