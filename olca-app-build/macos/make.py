import glob
import os
import shutil

BASE = "../build/macosx.cocoa.x86_64/openLCA/"


def main():
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


if __name__ == "__main__":
    main()
