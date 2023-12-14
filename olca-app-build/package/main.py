import argparse
import shutil

from package import PROJECT_DIR
from package.dir import DistDir, BuildDir, delete
from package.dist import Lib, OsArch, Version
from package.jre import JRE
from package.mac import MacDir
from package.native import NativeLib
from package.nsis import Nsis
from package.template import Template
from package.zipio import Zip


def package(
    osa: OsArch,
    version: Version,
    build_dir: BuildDir,
    win_installer: bool = False,
    mkl: bool = False,
):
    if osa.is_mac():
        MacDir.arrange(build_dir)

    # JRE and native libraries
    JRE.extract_to(build_dir)
    lib = Lib.MKL if mkl else Lib.BLAS
    NativeLib.extract_to(build_dir, lib)

    # edit the JRE Info.plist
    if osa.is_mac():
        MacDir.edit_jre_info(build_dir)

    # copy credits
    print("  Copying credits...")
    about_page = PROJECT_DIR / "credits/about.html"
    if about_page.exists():
        shutil.copy2(about_page, build_dir.about)
        plugin_dir = build_dir.olca_plugin
        if plugin_dir:
            shutil.copy2(about_page, plugin_dir)

    # copy ini and bin files
    bins: list[str] = []
    if osa.is_win():
        Template.apply(
            PROJECT_DIR / "templates/openLCA_win.ini",
            build_dir.app / "openLCA.ini",
            encoding="iso-8859-1",
            lang="en",
        )
        bins = ["ipc-server.cmd", "grpc-server.cmd"]
    if osa.is_linux():
        shutil.copy2(
            PROJECT_DIR / "templates/openLCA_linux.ini",
            build_dir.app / "openLCA.ini",
        )
        bins = ["ipc-server.sh", "grpc-server.sh"]
    if len(bins) > 0:
        bin_dir = build_dir.app / "bin"
        bin_dir.mkdir(exist_ok=True, parents=True)
        for binary in bins:
            bin_source = PROJECT_DIR / f"bin/{binary}"
            bin_target = bin_dir / binary
            if not bin_source.exists() or bin_target.exists():
                continue
            shutil.copy2(bin_source, bin_target)

    # build the package
    app_prefix = f"openLCA" if lib == Lib.BLAS else f"openLCA_{lib.value}"
    pack_name = f"{app_prefix}_{osa.value}_{version.app_suffix}"

    print(f"  Creating package {pack_name}...")
    pack = DistDir.get() / pack_name
    if osa == OsArch.WINDOWS_X64:
        shutil.make_archive(pack.as_posix(), "zip", build_dir.root.as_posix())
    else:
        Zip.targz(build_dir.root, pack)

    if osa.is_win() and win_installer:
        Nsis.run(build_dir, version, pack_name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-w",
        "--winstaller",
        help="also create an Windows installer",
        action="store_true",
    )
    parser.add_argument(
        "-m",
        "--mkl",
        help="package the MKL framework instead of OpenBLAS",
        action="store_true",
    )
    parser.add_argument(
        "-c",
        "--clean",
        help="delete the last build files",
        action="store_true"
    )
    args = parser.parse_args()

    # delete build resources
    DistDir.clean()
    if args.clean:
        for arch in OsArch:
            build_dir = BuildDir(arch)
            if build_dir.root.exists():
                print(f"delete: ${build_dir.root}")
                shutil.rmtree(build_dir.root)
            if build_dir.export_dir.exists():
                print(f"delete: ${build_dir.export_dir}")
                shutil.rmtree(build_dir.export_dir)
        return

    version = Version.get()
    for osa in OsArch:
        build_dir = BuildDir(osa)

        if not build_dir.export_dir.exists():
            print(f"No {osa} export is available; skipped")
            continue

        print(f"\nCopying the {osa.value} export...")
        build_dir.copy_export()

        print(f"Packaging the {osa.value} build...")
        package(osa, version, build_dir, args.winstaller, args.mkl)
        delete(build_dir.root)
        print(f"Done packaging the {osa.value} build.")


if __name__ == "__main__":
    main()
