
from pathlib import Path


class Template:

    @staticmethod
    def apply(
        source: Path, target: Path, encoding: str = "utf-8", **kwargs: str
    ):
        with open(source, mode="r", encoding="utf-8") as inp:
            template = inp.read()
            text = template.format(**kwargs)
        with open(target, "w", encoding=encoding) as out:
            out.write(text)
