# this script will probably replace the old packager script

import datetime
import glob
import os
import shutil
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
    # pack_win(version, version_date)
    pack_linux(version_date)

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
            tar = tarfile.open(jre_tar[0])
            tar.extractall(product_dir)
            tar.close()
            jre_dir = glob.glob(product_dir + '/*jre*')
            os.rename(jre_dir[0], p(product_dir + '/jre'))
            print('done')

    printw('  Create distribtuion package')
    dist_pack = p('build/dist/openLCA_linux64_%s.tar.gz' % version_date)
    with tarfile.open(dist_pack, 'w:gz') as tar:
        tar.add(p('build/linux.gtk.x86_64'), arcname='openLCA')
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


def p(path):
    """ Joins the given strings to a path """
    if os.sep != '/':
        return path.replace('/', os.sep)
    return path


def printw(msg: str):
    print(msg, end=' ... ', flush=True)


if __name__ == '__main__':
    main()
