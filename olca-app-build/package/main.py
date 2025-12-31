import argparse
import shutil
import zipfile

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
    plugins_dir = (
        build_dir.root / "openLCA/plugins"
        if osa.is_mac()
        else build_dir.app / "plugins"
    )
    BuildDir.unjar_plugins(plugins_dir)

    if osa.is_mac():
        MacDir.arrange(build_dir)

    # JRE and native libraries
    JRE.extract_to(build_dir)
    lib = Lib.MKL if mkl else Lib.BLAS
    # Skip native library bundling for macOS ARM64 (uses Accelerate Framework)
    if osa != OsArch.MACOS_ARM:
        NativeLib.extract_to(build_dir, lib)

    # Copy Accelerate sparse wrapper dylib for macOS ARM64
    if osa == OsArch.MACOS_ARM:
        print("  Copying Accelerate sparse wrapper library...")
        dylib_target_dir = build_dir.app / "native" / "osx-aarch64"
        dylib_target_dir.mkdir(parents=True, exist_ok=True)
        dylib_target = dylib_target_dir / "libaccelerate_sparse_wrapper.dylib"
        resource_path = "native/osx-aarch64/libaccelerate_sparse_wrapper.dylib"
        
        dylib_found = False
        
        # Try known locations first (from Maven build output)
        # 1. Workspace root (where pom.xml copies it for development)
        workspace_root = PROJECT_DIR.parent.parent / "native" / "osx-aarch64" / "libaccelerate_sparse_wrapper.dylib"
        if workspace_root.exists():
            shutil.copy2(workspace_root, dylib_target)
            dylib_target.chmod(0o755)
            print(f"  Copied Accelerate dylib from workspace root to {dylib_target}")
            dylib_found = True
        
        # 2. Maven build output (olca-core target/classes)
        if not dylib_found:
            maven_build = PROJECT_DIR.parent.parent / "olca-modules" / "olca-core" / "target" / "classes" / "native" / "osx-aarch64" / "libaccelerate_sparse_wrapper.dylib"
            if maven_build.exists():
                shutil.copy2(maven_build, dylib_target)
                dylib_target.chmod(0o755)
                print(f"  Copied Accelerate dylib from Maven build output to {dylib_target}")
                dylib_found = True
        
        # 3. Try extracting from olca-core JAR in plugins directory
        if not dylib_found:
            mac_plugins_dir = build_dir.app / "plugins"
            if mac_plugins_dir.exists():
                for item in mac_plugins_dir.iterdir():
                    if item.is_file() and item.suffix == ".jar" and "olca-core" in item.name.lower():
                        try:
                            with zipfile.ZipFile(item, 'r') as z:
                                if resource_path in z.namelist():
                                    dylib_target.write_bytes(z.read(resource_path))
                                    dylib_target.chmod(0o755)
                                    print(f"  Extracted Accelerate dylib from {item.name} to {dylib_target}")
                                    dylib_found = True
                                    break
                        except Exception as e:
                            print(f"  Warning: Failed to read JAR {item.name}: {e}")
        
        # 4. Try searching in olca-core directory in plugins
        if not dylib_found:
            mac_plugins_dir = build_dir.app / "plugins"
            if mac_plugins_dir.exists():
                for item in mac_plugins_dir.iterdir():
                    if item.is_dir() and "olca-core" in item.name.lower():
                        dylib_source = item / resource_path
                        if dylib_source.exists():
                            shutil.copy2(dylib_source, dylib_target)
                            dylib_target.chmod(0o755)
                            print(f"  Copied Accelerate dylib from {item.name} to {dylib_target}")
                            dylib_found = True
                            break
                        # Try recursive search
                        for dylib_file in item.rglob("libaccelerate_sparse_wrapper.dylib"):
                            shutil.copy2(dylib_file, dylib_target)
                            dylib_target.chmod(0o755)
                            print(f"  Copied Accelerate dylib from {dylib_file} to {dylib_target}")
                            dylib_found = True
                            break
                    if dylib_found:
                        break
        
        if not dylib_found:
            print(f"  Warning: Could not find Accelerate dylib")
            print(f"  Warning: Tried workspace root, Maven build output, and plugins directory")

    # edit the JRE Info.plist
    if osa.is_mac():
        MacDir.edit_jre_info(build_dir)

    # copy credits
    print("  Copying credits...")
    about_page = PROJECT_DIR / "credits/about.html"
    if about_page.exists():
        shutil.copy2(about_page, build_dir.about)
        plugins_dir = build_dir.olca_plugin
        if plugins_dir:
            shutil.copy2(about_page, plugins_dir)

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
        "-c", "--clean", help="delete the last build files", action="store_true"
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
