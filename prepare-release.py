# Checks and updates the resources that go into a final release. These are:
# * the olca-modules (we assume that the olca-modules repository is located
#   next to this repository)
# * the current reference database
# * the HTML pages
# * the current modules interface for the Jython interpreter

from os import name
from subprocess import call

def main():
    call('update_modules.bat')
    call('mvn.cmd package', cwd='./olca-refdata')
    call('npm.cmd install', cwd='./olca-app-html')
    call('npm.cmd run build', cwd='./olca-app-html')
    call('node gen-jython-bindings.js')


if __name__ == '__main__':
    main()
