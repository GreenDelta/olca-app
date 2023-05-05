# This script builds the `about.html` page that contains the license information
# and third party credits. This about page is cached in the `credits` folder
# and not put under version control. It is only created when not already
# present, thus, in order to update just delete the old version and run this
# script. When building the `about.html` pages the respective third party
# licenses are fetched from the respective websites and cached in the
# `credits/cache` folder. This cache folder is also not put under version
# control.

import json
import urllib.request as request

from dataclasses import dataclass
from pathlib import Path
from typing import Any

_dir = Path(__file__).parent


@dataclass
class ProjectInfo:
    name: str
    url: str
    license: str
    license_url: str

    @staticmethod
    def read_all() -> list["ProjectInfo"]:
        with open(_dir / "credits.json", "r", encoding="utf-8") as f:
            d: dict[str, Any] = json.load(f)
            infos = []
            for info in d["credits"]:
                infos.append(ProjectInfo.from_json(info))
            return infos

    @staticmethod
    def from_json(info: dict[str, str]) -> "ProjectInfo":
        return ProjectInfo(
            name=info["project"],
            url=info["projectUrl"],
            license=info["license"],
            license_url=info["licenseUrl"],
        )

    def get_license_text(self) -> str:
        cache_dir = _dir / "cache"
        cache_dir.mkdir(exist_ok=True, parents=True)
        cache_file = cache_dir / f"{self.name}_license.txt"
        if cache_file.exists():
            return cache_file.read_text()
        print(f"fetch license from {self.license_url}")
        with request.urlopen(self.license_url) as response:
            data = response.read()
            text = data.decode("utf-8")
            cache_file.write_text(text)
            return text


def main():
    about_html = _dir / "about.html"
    if about_html.exists():
        print("about.html alread exists; did nothing")
        return

    credits = ""
    for info in ProjectInfo.read_all():
        license = info.get_license_text()
        credits += f"""
        <div style='width: 100%; margin-bottom: 10px;'>
            <div class='block'>
                <div class='mr-pd'>
                <span>
                    {info.name}
                    (licensed under <a target="_blank" href="{info.license_url}">{info.license}</a>)
                </span>
                </div>
                <div style='display: flex; flex-direction: row;'>
                <div class='mr-pd pointer'>
                    <span data-action-id='{info.name}'>
                    show licence
                    </span>
                </div>
                <div class='mr-pd'>
                    <a target='_blank' href='{info.url}'>homepage</a>
                </div>
                </div>
            </div>
            <div data-description='licence-content' data-id='{info.name}' class='licence-details hide'>
                <div>
                <pre style='word-wrap: break-word; white-space: pre-wrap;'>{license}</pre>
                </div>
            </div>
        </div>"""

    about_template = _dir / "about_template.html"
    about_text = about_template.read_text().replace("#{credits}#", credits)
    about_html.write_text(about_text)


if __name__ == "__main__":
    main()
