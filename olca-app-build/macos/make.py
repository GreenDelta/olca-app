import glob
import os
import shutil
import subprocess

BASE = "../build/macosx.cocoa.x86_64/openLCA/"


def main():
    os.makedirs('../build/dist', exist_ok=True)
    os.makedirs(BASE + 'openLCA.app/Contents/Eclipse', exist_ok=True)
    os.makedirs(BASE + 'openLCA.app/Contents/MacOS', exist_ok=True)

    shutil.copyfile('Info.plist', BASE+'openLCA.app/Contents/Info.plist')
    shutil.move(BASE+"configuration", BASE + 'openLCA.app/Contents/Eclipse')
    shutil.move(BASE+"plugins", BASE + 'openLCA.app/Contents/Eclipse')
    shutil.move(BASE+".eclipseproduct", BASE + 'openLCA.app/Contents/Eclipse')
    shutil.move(BASE+"Resources", BASE+"openLCA.app/Contents")
    shutil.copyfile(BASE+"MacOS/openLCA", BASE +
                    'openLCA.app/Contents/MacOS/eclipse')

    # create the ini file
    plugins_dir = BASE + "openLCA.app/Contents/Eclipse/plugins/"
    launcher_jar = os.path.basename(
        glob.glob(plugins_dir + "*launcher*.jar")[0])
    launcher_lib = os.path.basename(
        glob.glob(plugins_dir + "*launcher.cocoa.macosx*")[0])
    with open("openLCA.ini", mode='r', encoding="utf-8") as f:
        text = f.read()
        text = text.format(launcher_jar=launcher_jar,
                           launcher_lib=launcher_lib)
        out_ini_path = BASE + "openLCA.app/Contents/Eclipse/eclipse.ini"
        with open(out_ini_path, mode='w', encoding='utf-8', newline='\n') as o:
            o.write(text)

    shutil.rmtree(BASE + "MacOS")
    os.remove(BASE + "Info.plist")

    # package the JRE
    jre_tar = glob.glob('../jre/jre-*-macosx-x64.tar')
    print(jre_tar)
    if len(jre_tar) == 0:
        print('ERROR: no JRE for Mac OSX found')
        return
    unzip(jre_tar[0], BASE + 'openLCA.app')
    jre_dir = glob.glob(BASE + 'openLCA.app' + '/*jre*')
    os.rename(jre_dir[0], BASE + 'openLCA.app' + '/jre')

    # package the native libraries
    if not os.path.exists('../julia/macos'):
        print('  WARNING: No native libraries')
    else:
        for f in glob.glob('../julia/macos/*.*'):
            shutil.copy2(f, BASE + 'openLCA.app/Contents/Eclipse')

    # targz(BASE + '*', '../build/dist/openLCA_macOS')


def unzip(zip_file, to_dir):
    """ Extracts the given file to the given folder using 7zip."""
    print('unzip %s to %s' % (zip_file, to_dir))
    if not os.path.exists(to_dir):
        os.makedirs(to_dir)
    zip_app = '..\\7zip\\7za.exe'
    cmd = [zip_app, 'x', zip_file, '-o%s' % to_dir]
    code = subprocess.call(cmd)
    print(code)


def targz(folder, tar_file):
    print('targz %s to %s' % (folder, tar_file))
    tar_app = '..\\7zip/7za.exe'
    cmd = [tar_app, 'a', '-ttar', tar_file + '.tar', folder]
    subprocess.call(cmd)
    cmd = [tar_app, 'a', '-tgzip', tar_file + '.tar.gz', tar_file + '.tar']
    subprocess.call(cmd)
    os.remove(tar_file + '.tar')


if __name__ == "__main__":
    main()
