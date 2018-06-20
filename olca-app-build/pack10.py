# this script will probably replace the old packager script

import datetime
import glob
import os
import shutil


def main():
    print('Create the openLCA distribution packages')

    # delete the old versions
    if os.path.exists('build/dist'):
        print('Delete the old packages under build/dist', end=' ... ')
        shutil.rmtree('build/dist', ignore_errors=True)
        mkdir('build/dist')
        print('done')

    # version and time
    version = get_version()
    now = datetime.datetime.now()
    version_date = '%s_%d-%02d-%02d' % (version, now.year,
                                        now.month, now.day)

    # create packages
    pack_win(version, version_date)

    print('All done\n')


def get_version():
    version = ''
    manifest = '../olca-app/META-INF/MANIFEST.MF'
    print('Read version from %s' % manifest, end=' ... ', flush=True)
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
    if not os.path.exists(product_dir):
        print('folder %s does not exist; skip Windows version' % product_dir)

    print('Create Windows package')

    # licenses
    print('  Copy licenses', end=' ... ')
    shutil.copy2(p('resources/OPENLCA_README.txt'), product_dir)
    if not os.path.exists(p(product_dir + '/licenses')):
        shutil.copytree(p('resources/licenses'), p(product_dir + '/licenses'))
    print('done')

    # jre
    jre_dir = p('jre/win64')
    if not os.path.exists(jre_dir):
        print('  WARNING: Java runtime not found in %s' % jre_dir)
    else:
        if not os.path.exists(p(product_dir + '/jre')):
            print('  Copy JRE', end=' ... ', flush=True)
            shutil.copytree(jre_dir, p(product_dir + '/jre'))
            print('done')

    # julia libs
    if not os.path.exists('julia/win64'):
        print('  WARNING: Julia libraries not found in julia/win64')
    else:
        print("  Copy Julia libraries", end=' ... ', flush=True)
        for f in glob.glob('julia/win64/*.*'):
            shutil.copy2(p(f), product_dir)
        print("done")


def mkdir(path):
    if os.path.exists(path):
        return
    try:
        os.mkdir(path)
    except Exception as e:
        print('Failed to create folder ' + path, e)


def p(path):
    """ Joins the given strings to a path """
    if os.sep != '/':
        return path.replace('/', os.sep)
    return path


if __name__ == '__main__':
    main()
