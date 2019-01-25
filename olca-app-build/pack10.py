# this script will probably replace the old packager script

import datetime
import glob
import os
import shutil
import subprocess
import tarfile

from os.path import exists


def main():
    print('Create the openLCA distribution packages')

    # delete the old versions
    if exists('build/dist'):
        print('Delete the old packages under build/dist', end=' ... ')
        shutil.rmtree('build/dist', ignore_errors=True)
        print('done')
    mkdir('build/dist')

    # version and time
    version = get_version()
    now = datetime.datetime.now()
    version_date = '%s_%d-%02d-%02d' % (version, now.year,
                                        now.month, now.day)

    # create packages
    pack_win(version, version_date)
    pack_linux(version_date)
    # pack_macos(version_date)

    print('All done\n')


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


def pack_win(version, version_date):
    product_dir = 'build/win32.win32.x86_64/openLCA'
    if not exists(product_dir):
        print('folder %s does not exist; skip Windows version' % product_dir)
        return

    print('Create Windows package')
    copy_licenses(product_dir)

    # jre
    jre_dir = p('jre/win64')
    if not exists(jre_dir):
        print('  WARNING: No JRE found %s' % jre_dir)
    else:
        if not exists(p(product_dir + '/jre')):
            printw('  Copy JRE')
            shutil.copytree(jre_dir, p(product_dir + '/jre'))
            print('done')

    # julia libs
    if not exists('julia/win64'):
        print('  WARNING: Julia libraries not found in julia/win64')
    else:
        printw("  Copy Julia libraries")
        for f in glob.glob('julia/win64/*.*'):
            shutil.copy2(p(f), product_dir)
        print('done')

    # zip file
    zip_file = p('build/dist/openLCA_win64_' + version_date)
    printw('  Create zip %s' % zip_file)
    shutil.make_archive(zip_file, 'zip', 'build/win32.win32.x86_64')
    print('done')

    # create installer
    pack_dir = 'build/win32.win32.x86_64'
    inst_files = glob.glob('resources/installer_static_win/*')
    for res in inst_files:
        if os.path.isfile(res):
            shutil.copy2(res, p(pack_dir + '/' + os.path.basename(res)))
    mkdir(p(pack_dir + '/english'))
    ini = fill_template(p('templates/openLCA_win.ini'),
                        lang='en', heap='3584M')
    with open(p(pack_dir + '/english/openLCA.ini'), 'w',
              encoding='iso-8859-1') as f:
        f.write(ini)
    mkdir(p(pack_dir + '/german'))
    ini_de = fill_template(p('templates/openLCA_win.ini'),
                           lang='de', heap='3584M')
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
                p('build/dist/openLCA_win64_' + version_date + ".exe"))


def pack_linux(version_date):
    product_dir = 'build/linux.gtk.x86_64/openLCA'
    if not exists(product_dir):
        print('folder %s does not exist; skip Linux version' % product_dir)

    print('Create Linux package')
    copy_licenses(product_dir)

    # package the JRE
    if not exists(product_dir + '/jre'):
        jre_tar = glob.glob('jre/jre-*linux*x64*.tar')
        if len(jre_tar) == 0:
            print('  WARNING: No Linux JRE found')
        else:
            printw('  Copy JRE')
            unzip(jre_tar[0], product_dir)
            jre_dir = glob.glob(product_dir + '/*jre*')
            os.rename(jre_dir[0], p(product_dir + '/jre'))
            print('done')

    # copy the ini file
    shutil.copy2('templates/openLCA_linux.ini', p(product_dir + "/openLCA.ini"))

    printw('  Create distribtuion package')
    dist_pack = p('build/dist/openLCA_linux64_%s' % version_date)
    targz('.\\build\\linux.gtk.x86_64\\*', dist_pack)
    print('done')


def pack_macos(version_date):
    product_dir = 'build/macosx.cocoa.x86_64/openLCA'
    if not exists(product_dir):
        print('folder %s does not exist; skip macOS version' % product_dir)

    print('Create macOS package')

    printw('Move folders around')
    app_dir = p(product_dir + '/' + 'openLCA.app')
    moves = ['configuration', 'plugins', '.eclipseproduct']
    for m in moves:
        move(p(product_dir + '/' + m), app_dir)
    move(p(product_dir + '/MacOS/openLCA'),
         p(app_dir + '/Contents/MacOS'))
    if exists(p(product_dir + '/MacOS/')):
        shutil.rmtree(p(product_dir + '/MacOS/'))
    move(p(product_dir + '/Resources'),
         p(app_dir + '/Contents'))
    move(p(product_dir + '/Info.plist'),
         p(app_dir + '/Contents'))
    print('done')

    # package the JRE
    if not exists(app_dir + '/jre'):
        jre_tar = glob.glob('jre/jre-*osx*x64*.tar')
        if len(jre_tar) == 0:
            print('  WARNING: No macOS JRE found')
        else:
            printw('  Copy JRE')
            unzip(jre_tar[0], app_dir)
            jre_dir = glob.glob(app_dir + '/*jre*')
            os.rename(jre_dir[0], p(app_dir + '/jre'))
            print('done')

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

    printw('  Create distribtuion package')
    dist_pack = p('build/dist/openLCA_macOS_%s' % version_date)
    targz('.\\build\\macosx.cocoa.x86_64\\*', dist_pack)
    print('done')


def copy_licenses(product_dir: str):
    # licenses
    printw('  Copy licenses')
    shutil.copy2(p('resources/OPENLCA_README.txt'), product_dir)
    if not exists(p(product_dir + '/licenses')):
        shutil.copytree(p('resources/licenses'), p(product_dir + '/licenses'))
    print('done')


def mkdir(path):
    if exists(path):
        return
    try:
        os.mkdir(path)
    except Exception as e:
        print('Failed to create folder ' + path, e)


def targz(folder, tar_file):
    print('targz %s to %s' % (folder, tar_file))
    tar_app = p('7zip/7za.exe')
    cmd = [tar_app, 'a', '-ttar', tar_file + '.tar', folder]
    subprocess.call(cmd)
    cmd = [tar_app, 'a', '-tgzip', tar_file + '.tar.gz', tar_file + '.tar']
    subprocess.call(cmd)
    os.remove(tar_file + '.tar')


def unzip(zip_file, to_dir):
    """ Extracts the given file to the given folder using 7zip."""
    print('unzip %s to %s' % (zip_file, to_dir))
    if not os.path.exists(to_dir):
        os.makedirs(to_dir)
    zip_app = p('7zip/7za.exe')
    cmd = [zip_app, 'x', zip_file, '-o%s' % to_dir]
    code = subprocess.call(cmd)
    print(code)


def move(f_path, target_dir):
    """ Moves the given file or directory to the given folder. """
    if not os.path.exists(f_path):
        # source file does not exist
        return
    base = os.path.basename(f_path)
    if os.path.exists(target_dir + '/' + base):
        # target file or dir already exsists
        return
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    shutil.move(f_path, target_dir)


def fill_template(file_path, **kwargs):
    with open(file_path, mode='r', encoding='utf-8') as f:
        text = f.read()
        return text.format(**kwargs)


def p(path):
    """ Joins the given strings to a path """
    if os.sep != '/':
        return path.replace('/', os.sep)
    return path


def printw(msg: str):
    print(msg, end=' ... ', flush=True)


if __name__ == '__main__':
    main()
