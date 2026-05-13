# Checks and updates the resources that go into a final release. These are:
# * the olca-modules (we assume that the olca-modules repository is located
#   next to this repository)
# * the current reference database
# * the HTML pages
# * the current modules interface for the Jython interpreter

import os
from subprocess import call

_is_posix = os.name == "posix"


def main():
    # note that the order of these steps is important
    call([cmd("mvn"), "-f", "pom_libs.xml", "clean"], cwd="./olca-app")
    mods_update = "./update_modules.sh" if _is_posix else "update_modules.bat"
    call(mods_update)
    call([cmd("mvn"), "package"], cwd="./olca-refdata")
    call([cmd("node"), "gen-jython-bindings.js"])
    call([cmd("npm"), "install"], cwd="./olca-app-html")
    call([cmd("npm"), "run", "build"], cwd="./olca-app-html")
    call([cmd("npm"), "install"], cwd="./olca-app-build/credits")
    call([cmd("node"), "credits-gen.js"], cwd="./olca-app-build/credits")


def cmd(cm: str) -> str:
    if _is_posix:
        return cm
    if cm == "mvn" or cm == "npm":
        return cm + ".cmd"
    return cm


if __name__ == "__main__":
    main()
