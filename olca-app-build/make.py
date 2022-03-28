# This script creates the openLCA distribution packages. It currently only works
# on Windows as it calls native binaries to create the packages (e.g. NSIS,
# 7zip). A Windows installer is only created when you pass the `--winstaller`
# flag into the script.

import datetime
import glob
import os

import requests
import shutil
import subprocess
import sys

from io import BytesIO
from os.path import exists
from pathlib import Path, PureWindowsPath
from pyunpack import Archive
from urlpath import URL
from zipfile import ZipFile

from fetch_runtime import OS, fetch_libs, fetch_jre, JRE_DIR, BLAS_DIR

BUILD_ROOT = Path(os.path.dirname(os.path.abspath(__file__)))
MANIFEST_DIR = BUILD_ROOT.parent / Path("olca-app/META-INF/MANIFEST.MF")

DIST_DIR = Path("build/dist")
RESOURCES_DIR = Path("resources")

NSIS_VERSION = "2.51"
NSIS_DIR = Path("tools/nsis-" + NSIS_VERSION)
NSIS_URL = URL("https://sourceforge.net/projects/nsis/files/NSIS%202/") / NSIS_VERSION / \
           ("nsis-" + NSIS_VERSION + ".zip") / "download"

SEVENZIP_DIR = Path("tools/7zip")
SEVENZIP_URL = URL("https://www.7-zip.org/a/7z2107-extra.7z")

WIN_DIR = Path("build/win32.win32.x86_64")
LINUX_DIR = Path("build/linux.gtk.x86_64")
MACOS_DIR = Path("build/macosx.cocoa.x86_64")


def main():
    print('Create the openLCA distribution packages')

    # delete the old versions
    if exists(DIST_DIR):
        print(f"Delete the old packages under {DIST_DIR}", end=' ... ')
        shutil.rmtree(DIST_DIR, ignore_errors=True)
        print('done')
    mkdir(DIST_DIR)

    # version and time
    version = get_version()
    now = datetime.datetime.now()
    version_date = '%s_%d-%02d-%02d' % (version, now.year,
                                        now.month, now.day)

    # create packages
    win_result = pack_win(version, version_date)
    linux_result = pack_linux(version_date)
    # macos_result = pack_macos(version_date)

    print("All done\n")
    print(f"Windows: {report(win_result)}")
    print(f"Linux: {report(linux_result)}")
    # print(f"MacOS: {report(macos_result)}")


def get_version() -> str:
    version = ''
    printw(f"Read version from {MANIFEST_DIR}")
    with open(MANIFEST_DIR, 'r', encoding='utf-8') as f:
        for line in f:
            text = line.strip()
            if not text.startswith('Bundle-Version'):
                continue
            version = text.split(':')[1].strip()
            break
    print("done version=%s" % version)
    return "2.0.0.0"


def pack_win(version: str, version_date: str) -> bool:
    _os = OS.WINDOWS
    product_dir = BUILD_ROOT / WIN_DIR / "openLCA"
    if not exists(product_dir):
        print(f"folder {product_dir} does not exist; skip Windows version")
        return False

    print('Create Windows package')
    copy_licenses(product_dir)

    # JRE
    if not exists(product_dir / "jre"):
        fetch_jre(_os)
        printw('  Copy JRE')
        shutil.copytree(JRE_DIR / _os.short(), product_dir / "jre")
        print('done')

    # BLAS
    if not exists(product_dir / "olca-native"):
        fetch_libs(_os)
        printw('  Copy native libraries')
        shutil.copytree(BLAS_DIR / _os.short(), product_dir / "olca-native")
        print('done')

    # zip file
    zip_file = DIST_DIR / ("openLCA_win64_" + version_date)
    printw(f"  Create zip {zip_file}")
    shutil.make_archive(zip_file.as_posix(), 'zip', WIN_DIR)
    print('done')

    # create installer when the `--winstaller` flag is set
    if '--winstaller' not in sys.argv:
        return False
    inst_files = glob.glob((RESOURCES_DIR / "installer_static_win/*").as_posix())
    for res in inst_files:
        if os.path.isfile(res):
            shutil.copy2(res, WIN_DIR / os.path.basename(res))
    mkdir(WIN_DIR / "english")
    ini = fill_template(file_path=Path("templates/openLCA_win.ini"),
                        lang='en',
                        heap='3584M')
    with open(file=WIN_DIR / "english/openLCA.ini",
              mode='w',
              encoding='iso-8859-1') as f:
        f.write(ini)
    mkdir(WIN_DIR / "german")
    ini_de = fill_template(file_path=Path("templates/openLCA_win.ini"),
                           lang='de',
                           heap='3584M')
    with open(file=WIN_DIR / "german/openLCA.ini",
              mode='w',
              encoding='iso-8859-1') as f:
        f.write(ini_de)
    setup = fill_template(file_path=Path("templates/setup.nsi"),
                          version=version)
    with open(WIN_DIR / "setup.nsi",
              mode='w',
              encoding='iso-8859-1') as f:
        f.write(setup)
    try:
        fetch_nsis()
    except OSError or ConnectionError as e:
        print(f"Failed to download NSIS, thus, not able to generate the Windows installer: {e}")
        return False
    cmd = [NSIS_DIR / "makensis.exe", WIN_DIR / "setup.nsi"]
    subprocess.call(cmd)
    shutil.move(src=WIN_DIR / "setup.exe",
                dst=DIST_DIR / Path("openLCA_win64_" + version_date + ".exe"))
    return True


def pack_linux(version_date: str) -> bool:
    _os = OS.LINUX
    product_dir = BUILD_ROOT / LINUX_DIR / "openLCA"
    if not exists(product_dir):
        print('folder %s does not exist; skip Linux version' % product_dir)
        return False

    print('Create Linux package')
    copy_licenses(product_dir)

    # JRE
    if not exists(product_dir / "jre"):
        fetch_jre(_os)
        printw('  Copy JRE')
        archive_path = BUILD_ROOT / JRE_DIR / _os.short() / _os.get_jre_name()
        try:
            fetch_7zip()
        except OSError or ConnectionError as e:
            print(f"Failed to download 7zip, thus, not able to generate the Linux installer: {e}")
            return False
        unzip(archive_path, product_dir / "jre")
        print('done')

    # BLAS
    fetch_libs(_os)
    if not exists(product_dir / "olca-native"):
        printw('  Copy native libraries')
        shutil.copytree(BLAS_DIR / _os.short(), product_dir / "olca-native")
        print('done')

    # copy the ini file
    shutil.copy2('templates/openLCA_linux.ini', product_dir / "openLCA.ini")

    printw('  Create distribution package')
    dist_pack = DIST_DIR / Path("openLCA_linux64_" + version_date)
    try:
        fetch_7zip()
    except OSError or ConnectionError as e:
        print(f"Failed to download 7zip, thus, not able to generate the Linux installer: {e}")
        return False
    targz(PureWindowsPath(LINUX_DIR), PureWindowsPath(dist_pack))
    print('done')
    return True


def pack_macos(version_date: str) -> bool:
    _os = OS.MACOS_X64
    product_dir = BUILD_ROOT / MACOS_DIR / "openLCA"
    if not exists(product_dir):
        print(f"folder {product_dir} does not exist; skip macOS version")
        return False
    print('Create macOS package')

    printw('Move folders around')

    os.makedirs(product_dir / 'openLCA.app/Contents/Eclipse', exist_ok=True)
    os.makedirs(product_dir / 'openLCA.app/Contents/MacOS', exist_ok=True)

    shutil.copyfile('macos/Info.plist', product_dir / 'openLCA.app/Contents/Info.plist')
    shutil.move(product_dir / "configuration", product_dir / 'openLCA.app/Contents/Eclipse')
    shutil.move(product_dir / "plugins", product_dir / 'openLCA.app/Contents/Eclipse')
    shutil.move(product_dir / ".eclipseproduct", product_dir / 'openLCA.app/Contents/Eclipse')
    shutil.move(product_dir / "Resources", product_dir / "openLCA.app/Contents")
    shutil.copyfile(product_dir / "MacOS/openLCA", product_dir / 'openLCA.app/Contents/MacOS/eclipse')

    # create the ini file
    plugins_dir = product_dir / "openLCA.app/Contents/Eclipse/plugins"
    launcher_jar = os.path.basename(
        glob.glob((plugins_dir / "*launcher*.jar").as_posix())[0])
    launcher_lib = os.path.basename(
        glob.glob((plugins_dir / "*launcher.cocoa.macosx*").as_posix())[0])
    with open("macos/openLCA.ini", mode='r', encoding="utf-8") as f:
        text = f.read()
        text = text.format(launcher_jar=launcher_jar,
                           launcher_lib=launcher_lib)
        out_ini_path = product_dir / "openLCA.app/Contents/Eclipse/eclipse.ini"
        with open(out_ini_path, mode='w', encoding='utf-8', newline='\n') as o:
            o.write(text)

    shutil.rmtree(product_dir / "MacOS")
    os.remove(product_dir / "Info.plist")

    # JRE
    if not exists(product_dir / "jre"):
        fetch_jre(_os)
        printw('  Copy JRE')
        archive_path = BUILD_ROOT / JRE_DIR / _os.short() / _os.get_jre_name()
        unzip(archive_path, product_dir / 'openLCA.app')
        print("done")

    printw('  Create distribution package')
    dist_pack = DIST_DIR / Path("openLCA_macOS_" + version_date)
    try:
        fetch_7zip()
    except OSError or ConnectionError as e:
        print(f"Failed to download 7zip, thus, not able to generate the Linux installer: {e}")
        return False
    targz(PureWindowsPath(MACOS_DIR / "openLCA"), PureWindowsPath(dist_pack))
    print('done')
    return True


def copy_licenses(product_dir: Path):
    # licenses
    printw('  Copy licenses')
    shutil.copy2(RESOURCES_DIR / "OPENLCA_README.txt", product_dir)
    if not exists(product_dir / "licenses"):
        shutil.copytree(RESOURCES_DIR / "licenses", product_dir / "licenses")
    print('done')


def mkdir(path: Path):
    if exists(path):
        return
    try:
        os.mkdir(path)
    except Exception as e:
        print(f"Failed to create folder {path}", e)


def targz(folder: PureWindowsPath, tar_file: PureWindowsPath):
    printw(f"  Making a targz archive of {folder} to {tar_file}.")
    tar_app = SEVENZIP_DIR / "7za.exe"
    cmd = [tar_app, 'a', '-ttar', tar_file.with_suffix(".tar"), folder / "*"]
    subprocess.call(cmd)
    cmd = [tar_app, 'a', '-tgzip', tar_file.with_suffix(".tar.gz"), tar_file.with_suffix(".tar")]
    subprocess.call(cmd)
    os.remove(tar_file.with_suffix(".tar"))
    print("done")


def unzip(zip_file: Path, to_dir: Path):
    """ Extracts the given file to the given folder using 7zip."""
    printw(f"  Unzip {zip_file} to {to_dir}.")
    if not os.path.exists(to_dir):
        os.makedirs(to_dir)
    zip_app = SEVENZIP_DIR / "7za.exe"
    cmd = [zip_app.as_posix(), 'x', zip_file.as_posix(), f"-o{to_dir.as_posix()}"]
    subprocess.call(cmd)
    print("done")


def fill_template(file_path: Path, **kwargs):
    with open(file_path, mode='r', encoding='utf-8') as f:
        text = f.read()
        return text.format(**kwargs)


def check_nsis():
    directory = NSIS_DIR
    if not os.path.isdir(directory):
        printw(f"  Creating the directory: {directory}")
        Path(directory).mkdir(parents=True, exist_ok=True)
        print("done")
        return False
    paths_list = directory.glob('*.exe')
    files = [path.name for path in paths_list if path.is_file()]
    return "makensis.exe" in files


def fetch_nsis():
    if check_nsis():
        return

    printw(f"  Downloading NSIS archive.")
    try:
        r = requests.get(NSIS_URL, allow_redirects=True, stream=True)
    except OSError as e:
        print(f"Failed to download {NSIS_URL} due to: \n{e}")
        raise e

    if not r.status_code == 200:
        print(f"<Error {r.status_code}> Failed to download {NSIS_URL}.")
        raise ConnectionError

    zipfile = ZipFile(BytesIO(r.content))
    zipfile.extractall(NSIS_DIR.parent)
    print("done")


def check_7zip():
    directory = SEVENZIP_DIR
    if not os.path.isdir(directory):
        printw(f"  Creating the directory: {directory}")
        Path(directory).mkdir(parents=True, exist_ok=True)
        print("done")
        return False
    paths_list = directory.glob('*.exe')
    files = [path.name for path in paths_list if path.is_file()]
    return "7za.exe" in files


def fetch_7zip():
    if check_7zip():
        return

    printw(f"  Downloading 7zip archive.")
    archive_name = "7zip.7z"
    target_path = SEVENZIP_DIR / archive_name
    try:
        r = requests.get(SEVENZIP_URL, allow_redirects=True, stream=True)
    except OSError as e:
        print(f"Failed to download {SEVENZIP_URL} due to: \n{e}")
        raise e

    if not r.status_code == 200:
        print(f"<Error {r.status_code}> Failed to download {SEVENZIP_URL}.")
        raise ConnectionError

    open(target_path, 'wb').write(r.content)

    Archive(target_path).extractall(SEVENZIP_DIR)

    print("done")


def printw(msg: str):
    print(msg, end=' ... ', flush=True)


def report(result: bool) -> str:
    if result:
        return "Success"
    else:
        return "Fail"


if __name__ == '__main__':
    main()
