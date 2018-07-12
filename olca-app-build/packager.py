# This script creates the openLCA distribution packages. It currently only
# works on Windows as it calls native binaries to create the Windows installers
# (e.g. NSIS).

import datetime
import glob
import os
import subprocess
import shutil

from os.path import exists


def main():
    print('Create the distribution packages')
    if exists('packages'):
        shutil.rmtree('packages', ignore_errors=True)
        mkdir('packages')
    now = datetime.datetime.now()
    version = get_version()
    version_date = '%s_%d-%02d-%02d' % (version, now.year,
                                        now.month, now.day)
    pack_linux(version_date)
    pack_macos(version_date)
    pack_win('win32', version_date, version)
    pack_win('win64', version_date, version)


def pack_win(arch, version_date, version):
    if arch != 'win32' and arch != 'win64':
        print('ERROR: unknown Windows arch: %s' % arch)
        return

    print('Package the %s distribution' % arch)

    # setting the build variables
    build_zip = '.win32.x86.zip'
    pack_dir = 'packages/' + arch
    app_dir = p(pack_dir + '/openLCA')
    heap_memory = '1024M'
    jre_dir = p('jre/win32')
    if arch == 'win64':
        build_zip = '.win32.x86_64.zip'
        heap_memory = '3248M'
        jre_dir = p('jre/win64')

    # extract the build
    app_pack = glob.glob('builds/openlca_*' + build_zip)
    if len(app_pack) == 0:
        print('ERROR: could not find build package', build_zip)
        return
    unzip(app_pack[0], pack_dir)

    # copy ini and licenses
    ini = fill_template(p('templates/openLCA_win.ini'),
                        lang='en', heap=heap_memory)
    with open(p(app_dir + '/openLCA.ini'), 'w', encoding='iso-8859-1') as f:
        f.write(ini)
    shutil.copy2(p('resources/OPENLCA_README.txt'), app_dir)
    shutil.copytree(p('resources/licenses'), p(app_dir + '/licenses'))

    # package the JRE
    shutil.copytree(jre_dir, p(app_dir + '/jre'))
    # julia libs
    if arch == 'win64':
        if exists('julia/win64'):
            for f in glob.glob('julia/win64/*.*'):
                shutil.copy2(p(f), p(app_dir))

    # create the non-installer zip
    print('Create the %s zip ...' % arch)
    shutil.make_archive(p('packages/openLCA_' + arch + '_' + version_date),
                        'zip', pack_dir)

    # create the win32 installer
    inst_files = glob.glob('resources/installer_static_win/*')
    for res in inst_files:
        if os.path.isfile(res):
            shutil.copy2(res, p(pack_dir + '/' + os.path.basename(res)))
    mkdir(p(pack_dir + '/english'))
    with open(p(pack_dir + '/english/openLCA.ini'), 'w',
              encoding='iso-8859-1') as f:
        f.write(ini)
    mkdir(p(pack_dir + '/german'))
    ini_de = fill_template(p('templates/openLCA_win.ini'),
                           lang='de', heap=heap_memory)
    with open(p(pack_dir + '/german/openLCA.ini'), 'w',
              encoding='iso-8859-1') as f:
        f.write(ini_de)
    setup = fill_template('templates/setup.nsi', version=version)
    with open(p(pack_dir + '/setup.nsi'), 'w',
              encoding='iso-8859-1') as f:
        f.write(setup)
    cmd = [p('nsis-2.46/makensis.exe'), p(pack_dir + '/setup.nsi')]
    subprocess.call(cmd)
    shutil.move(p(pack_dir + '/setup.exe'),
                p('packages/openLCA_' + arch + '_installer_' +
                  version_date + ".exe"))
    shutil.rmtree(pack_dir)


def pack_linux(version_date):
    app_pack = glob.glob('builds/openlca_*-linux.gtk.x86_64.zip')
    if len(app_pack) == 0:
        print('ERROR: could not find Linux package')
        return
    unzip(app_pack[0], p('packages/linux'))
    app_dir = p('packages/linux/openLCA')

    # copy ini and licenses
    shutil.copy2(p('templates/openLCA_linux.ini'), p(app_dir + '/openLCA.ini'))
    shutil.copy2(p('resources/OPENLCA_README.txt'), app_dir)
    shutil.copytree(p('resources/licenses'), p(app_dir + '/licenses'))

    # package the JRE
    jre_tar = glob.glob('jre/jre-*-linux-x64.tar')
    if len(jre_tar) == 0:
        print('ERROR: no JRE for Linux found')
        return
    unzip(jre_tar[0], app_dir)
    jre_dir = glob.glob(app_dir + '/*jre*')
    os.rename(jre_dir[0], p(app_dir + '/jre'))

    # copy Julia libs
    if exists('julia/linux'):
        for f in glob.glob('julia/linux/*.*'):
            shutil.copy2(p(f), p(app_dir))

    targz('.\\packages\\linux\\*',
          p('packages/openLCA_linux_' + version_date))
    shutil.rmtree(p('packages/linux'))


def pack_macos(version_date):
    app_pack = glob.glob('builds/openlca_*-macosx.cocoa.x86_64.zip')
    if len(app_pack) == 0:
        print('ERROR: could not find macOS package')
        return
    unzip(app_pack[0], p('packages/macos'))
    # TODO: delete `p2/*/.lock` files
    app_dir = p('packages/macos/openLCA/openLCA.app')
    moves = ['configuration', 'p2', 'plugins', '.eclipseproduct',
             'artifacts.xml']
    for m in moves:
        move(p('packages/macos/openLCA/' + m), app_dir)

    # package the JRE
    jre_tar = glob.glob('jre/jre-*-macosx-x64.tar')
    if len(jre_tar) == 0:
        print('ERROR: no JRE for Mac OSX found')
        return
    unzip(jre_tar[0], app_dir)
    jre_dir = glob.glob(app_dir + '/*jre*')
    os.rename(jre_dir[0], p(app_dir + '/jre'))

    # write the ini file
    launcher_jar = os.path.basename(
        glob.glob(app_dir + '/plugins/*launcher*.jar')[0])
    launcher_lib = os.path.basename(
        glob.glob(app_dir + '/plugins/*launcher.cocoa.macosx*')[0])
    ini = fill_template(p('templates/openLCA_mac.ini'),
                        launcher_jar=launcher_jar, launcher_lib=launcher_lib)
    ini_file = p(app_dir + '/Contents/MacOS/openLCA.ini')
    with(open(ini_file, 'w', encoding='utf-8', newline='\n')) as f:
        f.write(ini.replace('\r\n', '\n'))

    # create the distribution package
    targz('.\\packages\\macos\\openLCA\\*',
          p('packages/openLCA_macOS_' + version_date))
    shutil.rmtree(p('packages/macos'))


def unzip(zip_file, to_dir):
    """ Extracts the given file to the given folder using 7zip."""
    print('unzip %s to %s' % (zip_file, to_dir))
    if not exists(to_dir):
        os.makedirs(to_dir)
    zip_app = p('7zip/7za.exe')
    cmd = [zip_app, 'x', zip_file, '-o%s' % to_dir]
    code = subprocess.call(cmd)
    print(code)


def move(f_path, target_dir):
    """ Moves the given file or directory to the given folder. """
    if not exists(f_path):
        print('File or folder %s does not exist' % f_path)
        return
    print('Move %s to %s' % (f_path, target_dir))
    if not exists(target_dir):
        os.makedirs(target_dir)
    shutil.move(f_path, target_dir)


def check(code, msg):
    if code != 0:
        print('ERROR: exit code=%s; %s', msg)


def targz(folder, tar_file):
    print('targz %s to %s' % (folder, tar_file))
    tar_app = p('7zip/7za.exe')
    cmd = [tar_app, 'a', '-ttar', tar_file + '.tar', folder]
    subprocess.call(cmd)
    cmd = [tar_app, 'a', '-tgzip', tar_file + '.tar.gz', tar_file + '.tar']
    subprocess.call(cmd)
    os.remove(tar_file + '.tar')


def get_version():
    version = ''
    manifest = '../olca-app/META-INF/MANIFEST.MF'
    printw('Read version from %s' % manifest)
    with open(manifest, 'r', encoding='utf-8') as f:
        for line in f:
            text = line.strip()
            if not text.startswith('Bundle-Version'):
                continue
            version = text.split(':')[1].strip()
            break
    print("done version=%s" % version)
    return version


def fill_template(file_path, **kwargs):
    with open(file_path, mode='r', encoding='utf-8') as f:
        text = f.read()
        return text.format(**kwargs)


def p(path):
    """ Joins the given strings to a path """
    if os.sep != '/':
        return path.replace('/', os.sep)
    return path


def mkdir(path):
    if exists(path):
        return
    try:
        os.mkdir(path)
    except Exception as e:
        print('Failed to create folder ' + path, e)


def printw(msg: str):
    print(msg, end=' ... ', flush=True)


if __name__ == '__main__':
    main()
