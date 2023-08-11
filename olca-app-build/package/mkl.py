import os
import shutil
import urllib.request

from enum import Enum
from pathlib import Path
from typing import NamedTuple

from package.dist import Lib, OsArch
from package.zipio import Zip


class MathLib(NamedTuple):
        name: str
        win_url: str
        mac_url: str
        linux_url: str


class MKLFramework(Enum):

    MKL = MathLib(
        name="mkl-2023.1.0",
        win_url="https://files.pythonhosted.org/packages/d9/a1/b7cfb6f3e7259f035a2c947cf26bff42cda6772933cdb95c829e91ce995f/mkl-2023.1.0-py2.py3-none-win_amd64.whl",  # noqa
        mac_url="https://files.pythonhosted.org/packages/31/7f/e865657b372f8f0aa4664ea2d07a5f80a4aeb337760d571cc690011dc2ce/mkl-2023.1.0-py2.py3-none-macosx_10_15_x86_64.macosx_11_0_x86_64.whl",  # noqa
        linux_url="https://files.pythonhosted.org/packages/85/66/815fb18860ad600695008f1a5acfc14a3e3b09fd77f006e332ce61af1f40/mkl-2023.1.0-py2.py3-none-manylinux1_x86_64.whl",  # noqa
    )

    OPENMP = MathLib(
        name="intel_openmp-2023.1.0",
        win_url="https://files.pythonhosted.org/packages/c7/a1/7407ebfb7131c2d8507bec5daf0684d76a9c6c38faaa6ae16b296e3335ce/intel_openmp-2023.1.0-py2.py3-none-win_amd64.whl",  # noqa
        mac_url="https://files.pythonhosted.org/packages/3f/71/72f38f9340420e3a1456834ddd88442be97476174e4a24a1cc30d834659b/intel_openmp-2023.1.0-py2.py3-none-macosx_10_15_x86_64.macosx_11_0_x86_64.whl",  # noqa
        linux_url="https://files.pythonhosted.org/packages/a3/6d/08040c4cfab1997f3a104238d850ab0ac345356762e34be7b415c7544162/intel_openmp-2023.1.0-py2.py3-none-manylinux1_x86_64.whl",  # noqa
    )
    TBB = MathLib(
        name="tbb-2021.9.0",
        win_url="https://files.pythonhosted.org/packages/64/6a/20f2e84e31bd82b7ddecf616be0338b7fa5dc37285a73e810101f9c2b195/tbb-2021.9.0-py3-none-win_amd64.whl",  # noqa
        mac_url="https://files.pythonhosted.org/packages/b4/44/de6ad155a9b4c916cf72d3ad34de3c7802c51425b93e4727d1a372f9fb77/tbb-2021.9.0-py2.py3-none-macosx_10_15_x86_64.macosx_11_0_x86_64.whl",  # noqa
        linux_url="https://files.pythonhosted.org/packages/96/5f/aaae879605e95e147b7269e54a5b49654a44d6fee7fed54ece8f77d77ded/tbb-2021.9.0-py2.py3-none-manylinux1_i686.whl"  # noqa
    )

    def file_name(self, osa: OsArch):
        return f"{self.value.name}-py2.py3-none-${self.wheel_suffix(osa)}"

    @staticmethod
    def wheel_suffix(osa: OsArch):
        if osa == OsArch.MACOS_X64:
            return "macosx_10_15_x86_64.macosx_11_0_x86_64.whl"
        elif osa == OsArch.LINUX_X64:
            return "manylinux1_x86_64.whl"
        elif osa == OsArch.WINDOWS_X64:
            return "win_amd64.whl"
        else:
            raise ValueError(f"Unsupported OS+arch: {osa.value} for MKL.")

    def url(self, osa: OsArch):
        if osa == OsArch.MACOS_X64:
            return self.value.mac_url
        elif osa == OsArch.LINUX_X64:
            return self.value.linux_url
        elif osa == OsArch.WINDOWS_X64:
            return self.value.win_url
        else:
            raise ValueError(f"Unsupported OS+arch: {osa.value} for MKL.")

    @staticmethod
    def extract_to(dir: Path, osa: OsArch):
        if not dir.exists():
            dir.mkdir(parents=True, exist_ok=True)

        print("  Copying MKL libraries")

        for lib in MKLFramework:
            wheel = lib.fetch(osa)
            folder = Lib.MKL.cache_dir() / wheel.name[0:-3]
            if not folder.exists():
                Zip.unzip(wheel, folder)

            print(f"  Copying {lib.name} from {folder.name}")
            MKLFramework.copy_binaries(folder, dir)

    @staticmethod
    def copy_binaries(folder: Path, lib_dir: Path):
        # iterate over files in the wheel
        for root, _, files in os.walk(folder):
            for filename in files:
                file = Path(root) / filename
                path_patterns = [Path("/data/Library/bin/"), Path("/data/lib/")]
                if any(str(pattern) in str(file) for pattern in path_patterns):
                    shutil.copy2(file, lib_dir / filename)


    def fetch(self, osa: OsArch) -> Path:
        file = Lib.MKL.cache_dir() / self.file_name(osa)
        if os.path.exists(file):
            return file

        url = self.url(osa)

        print(f"  Fetching {self.name} from {url}...")
        urllib.request.urlretrieve(url, file)
        if not os.path.exists(file):
            raise AssertionError(f"{self.name} download failed; URL: {url}")
        return file
