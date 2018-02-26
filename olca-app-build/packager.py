import glob
import os
import subprocess
import shutil


def main():
    print('Create the distribution packages')
    if os.path.exists('packages'):
        shutil.rmtree('packages', ignore_errors=True)
        os.mkdir('packages')
    pack_macos()


def pack_macos():
    app_pack = glob.glob('builds/openlca_*-macosx.cocoa.x86_64.zip')
    if len(app_pack) == 0:
        print('ERROR: could not find macOS package')
        return
    pack_dir = os.path.join('packages', 'macos')
    unzip(app_pack[0], pack_dir)
    # TODO: delete `p2/*/.lock` files
    app_dir = os.path.join(pack_dir, 'openLCA', 'openLCA.app')
    moves = ['configuration', 'p2', 'plugins', '.eclipseproduct',
             'artifacts.xml']
    for m in moves:
        move(os.path.join(pack_dir, 'openLCA', m), app_dir)
    jre_tar = glob.glob('runtime/jre/jre-*-macosx-x64.tar')
    if len(jre_tar) == 0:
        print('ERROR: no JRE for Mac OSX found')
        return

    # package the JRE
    unzip(jre_tar[0], app_dir)
    jre_dir = glob.glob(app_dir + '/*jre*')
    os.rename(jre_dir[0], os.path.join(app_dir, 'jre'))

    # write the ini file
    launcher_jar = os.path.basename(
        glob.glob(app_dir + '/plugins/*launcher*.jar')[0])
    launcher_lib = os.path.basename(
        glob.glob(app_dir + '/plugins/*launcher.cocoa.macosx*')[0])
    ini = """-startup
../../plugins/%s
--launcher.library
../../plugins/%s
-nl
en
-data
@noDefault
-vm
../../jre/Contents/Home/lib/jli/libjli.dylib
-vmargs
-Xmx3248M
-Dosgi.framework.extensions=org.eclipse.fx.osgi
-XstartOnFirstThread
-Dorg.eclipse.swt.internal.carbon.smallFonts
""" % (launcher_jar, launcher_lib)
    ini_file = os.path.join(app_dir, 'Contents', 'MacOS', 'openLCA.ini')
    with(open(ini_file, 'w', encoding='utf-8', newline='\n')) as f:
        f.write(ini.replace('\r\n', '\n'))

    # create the distribution package
    targz('.\\packages\\macos\\openLCA\\*',
          os.path.join('packages', 'openLCA_macOS_1.7.0'))
    shutil.rmtree(pack_dir)


def unzip(zip_file, to_dir):
    """ Extracts the given file to the given folder using 7zip."""
    print('unzip %s to %s' % (zip_file, to_dir))
    if not os.path.exists(to_dir):
        os.makedirs(to_dir)
    zip_app = os.path.join('7zip', '7za.exe')
    cmd = [zip_app, 'x', zip_file, '-o%s' % to_dir]
    code = subprocess.call(cmd)
    print(code)


def move(f_path, target_dir):
    """ Moves the given file or directory to the given folder. """
    if not os.path.exists(f_path):
        print('File or folder %s does not exist' % f_path)
        return
    print('Move %s to %s' % (f_path, target_dir))
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    shutil.move(f_path, target_dir)
    # cmd = ['move', f_path, target_dir]
    # code = subprocess.call(cmd)


def delete(f_path):
    "Deletes the given file or folder"
    if not os.path.exists(f_path):
        return
    if os.path.isfile(f_path):
        print('Delete file %s' % f_path)
        cmd = ['del', '/Q', f_path]
        code = subprocess.call(cmd)
        check(code, "Failed to delete file %s" % f_path)
    elif os.path.isdir:
        print('Delete folder %s' % f_path)
        cmd = ['rmdir', '/s', '/q', f_path]
        code = subprocess.call(cmd)
        check(code, "Failed to delete folder %s" % f_path)


def check(code, msg):
    if code != 0:
        print('ERROR: exit code=%s; %s', msg)


def targz(folder, tar_file):
    print('targz %s to %s' % (folder, tar_file))
    tar_app = os.path.join('7zip', '7za.exe')
    cmd = [tar_app, 'a', '-ttar', tar_file + '.tar', folder]
    subprocess.call(cmd)
    cmd = [tar_app, 'a', '-tgzip', tar_file + '.tar.gz', tar_file + '.tar']
    subprocess.call(cmd)


if __name__ == '__main__':
    main()
