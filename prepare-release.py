# Checks and updates the resources that go into a final release. These are:
# * the olca-modules (we assume that the olca-modules repository is located
#   next to this repository)
# * the current reference database
# * the HTML pages
# * the current modules interface for the Jython interpreter

import argparse
import os
import re
import sys
from pathlib import Path
from subprocess import check_call

_is_posix = os.name == "posix"
_root_dir = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command")

    # register argument parsers
    bump_parser = subparsers.add_parser(
        "bump-version",
        help="update the Tycho Maven and RCP versions",
    )
    bump_parser.add_argument(
        "version",
        help="version like 2.6.2 or 2.6.3-SNAPSHOT",
    )
    subparsers.add_parser(
        "full-build",
        help="run release preparation, Tycho build, and full packaging",
    )

    # execute specific commands
    args = parser.parse_args()
    if args.command == "bump-version":
        bump_version(args.version)
        return
    if args.command == "full-build":
        full_build()
        return

    # run the release preparation per default
    prepare_release()


def prepare_release():
    # note that the order of these steps is important
    check_call([cmd("mvn"), "-f", "pom_libs.xml", "clean"], cwd="./olca-app")
    mods_update = "./update_modules.sh" if _is_posix else "update_modules.bat"
    check_call(mods_update)
    check_call([cmd("mvn"), "package"], cwd="./olca-refdata")
    check_call([cmd("node"), "gen-jython-bindings.js"])
    check_call([cmd("npm"), "install"], cwd="./olca-app-html")
    check_call([cmd("npm"), "run", "build"], cwd="./olca-app-html")
    check_call([cmd("npm"), "install"], cwd="./olca-app-build/credits")
    check_call([cmd("node"), "credits-gen.js"], cwd="./olca-app-build/credits")


def bump_version(version: str):
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-SNAPSHOT)?", version):
        raise SystemExit(
            "version must be <major>.<minor>.<patch> or"
            " <major>.<minor>.<patch>-SNAPSHOT"
        )

    mvn = cmd("mvn")
    tycho_version = get_tycho_version()
    tycho_versions_plugin = (
        f"org.eclipse.tycho:tycho-versions-plugin:{tycho_version}"
    )
    set_version_goal = f"{tycho_versions_plugin}:set-version"
    update_metadata_goal = f"{tycho_versions_plugin}:update-eclipse-metadata"
    build_dir = _root_dir / "olca-app-build"

    check_call(
        [mvn, set_version_goal, f"-DnewVersion={version}"],
        cwd=build_dir,
    )
    check_call([mvn, update_metadata_goal], cwd=build_dir)


def full_build():
    prepare_release()
    build_dir = _root_dir / "olca-app-build"
    check_call([cmd("mvn"), "clean", "verify"], cwd=build_dir)
    check_call(
        [python_cmd(), "-m", "package", "--mkl", "--winstaller"], cwd=build_dir
    )


def get_tycho_version() -> str:
    """Get the Tycho version from the POM in the build folder."""
    pom = (_root_dir / "olca-app-build/pom.xml").read_text(encoding="utf-8")
    match = re.search(r"<tycho.version>([^<]+)</tycho.version>", pom)
    if match is None:
        raise SystemExit(
            "failed to read tycho.version from olca-app-build/pom.xml"
        )
    return match.group(1).strip()


def cmd(cm: str) -> str:
    if _is_posix:
        return cm
    if cm == "mvn" or cm == "npm":
        return cm + ".cmd"
    return cm


def python_cmd() -> str:
    if os.name == "nt":
        return "py"
    return sys.executable


if __name__ == "__main__":
    main()
