import json
import re
from pathlib import Path

mod_path = Path("../olca-app/src/org/openlca/app/devtools/python/mod_bindings.py")
app_path = Path("../olca-app/src/org/openlca/app/devtools/python/app_bindings.py")

mod_lines = mod_path.read_text(encoding="utf-8").splitlines()
app_lines = app_path.read_text(encoding="utf-8").splitlines()

lines = mod_lines + app_lines

completions = []
for line in lines:
    if line.startswith("import "):
        as_match = re.search(r"as\s+(\w+)$", line)
        if not as_match:
            continue
        label = as_match.group(1)
        detail_match = re.search(r"import\s+([a-zA-Z0-9_.]+)\.[A-Z]", line)
        detail = detail_match.group(1) if detail_match else None
        completions.append({"label": label, "type": "class", "detail": detail})

with open("olca-completions.json", "w", encoding="utf-8") as f:
    json.dump(completions, f, indent=2)
