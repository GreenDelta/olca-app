# Checks and updates the resources that go into a final release. These are:
# * the olca-modules (we assume that the olca-modules repository is located
#   next to this repository)
# * the current reference database
# * the HTML pages
# * the current modules interface for the Jython interpreter

import os
from subprocess import call


def main():
    if os.name == 'posix':
        call(['mvn', 'clean'], cwd='./olca-app')
        call('./update_modules.sh')
        call(['mvn', 'package'], cwd='./olca-refdata')
        call(['npm', 'install'], cwd='./olca-app-html')
        call(['npm', 'run', 'build'], cwd='./olca-app-html')
        call(['node', 'gen-jython-bindings.js'])
    else:
        call('mvn.cmd clean', cwd='./olca-app')
        call('update_modules.bat')
        call('mvn.cmd package', cwd='./olca-refdata')
        call('npm.cmd install', cwd='./olca-app-html')
        call('npm.cmd run build', cwd='./olca-app-html')
        call('node gen-jython-bindings.js')


if __name__ == '__main__':
    main()
