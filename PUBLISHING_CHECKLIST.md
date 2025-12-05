# Publishing Checklist for openLCA EcoSpold2 Fork

## Pre-Publishing Steps

### 1. Clean Repository
- [x] `.gitignore` includes `sample/` folder
- [x] Verify no proprietary ecoinvent data in repo
- [x] README.md updated with fork information
- [x] ECOSPOLD2_IMPORT_GUIDE.md created with instructions

### 2. Code Changes Summary
Files modified:
- `olca-app/plugin.xml` - Uncommented EcoSpold2ImportWizard registration
- `olca-app/src/org/openlca/app/tools/FileImport.java` - Enabled EcoSpold2 wizard invocation
- `ECOSPOLD2_IMPORT_GUIDE.md` - User documentation (includes Python script)
- `README.md` - Fork description and differences

### 3. Legal Compliance
- ✅ No ecoinvent data files included
- ✅ `sample/` folder in .gitignore
- ✅ Documentation clearly states users must obtain ecoinvent license
- ✅ Python script is generic tool (no proprietary data)

## Publishing Options

### Option A: GitHub Release (Recommended)

**Steps:**
```bash
# 1. Commit changes
git add .gitignore README.md ECOSPOLD2_IMPORT_GUIDE.md olca-app/plugin.xml olca-app/src/
git commit -m "Enable EcoSpold2 import functionality

- Re-enable EcoSpold2ImportWizard in plugin.xml
- Update FileImport.java to invoke EcoSpold2 wizard
- Add comprehensive import guide with flow mapping script
- No proprietary data included"

# 2. Push to GitHub
git push origin master

# 3. Create annotated tag
git tag -a v2.5.1-ecospold2 -m "openLCA 2.5.1 with EcoSpold2 import enabled"
git push origin v2.5.1-ecospold2

# 4. Create GitHub Release
# Go to: https://github.com/ankur-paan/olca-app/releases/new
# - Tag: v2.5.1-ecospold2
# - Title: openLCA 2.5.1 - EcoSpold2 Import Enabled
# - Description: Use template below
```

**Release Description Template:**
```markdown
# openLCA 2.5.1 - EcoSpold2 Import Enabled

This is an unofficial fork of openLCA with EcoSpold2 import functionality restored.

## Changes from Official Release

✅ **Re-enabled EcoSpold2 import wizard**
- Available via: File → Import → Other → EcoSpold 2
- Supports .spold, .xml, and .zip files
- Drag-drop and double-click support

✅ **Flow mapping support**
- Optional CSV-based flow mapping
- Python script to generate mappings from your ecoinvent dataset
- Auto-creates missing flows if no mapping provided

## Installation

### Download Pre-built Binaries (Coming Soon)
Windows/Linux/macOS packages will be provided in future releases.

### Build from Source
See [README.md](https://github.com/ankur-paan/olca-app/blob/master/README.md) for build instructions.

## Usage Guide

📖 **Read the complete guide:** [ECOSPOLD2_IMPORT_GUIDE.md](https://github.com/ankur-paan/olca-app/blob/master/ECOSPOLD2_IMPORT_GUIDE.md)

**Quick Start:**
1. Obtain EcoSpold2 dataset from ecoinvent.org (requires license)
2. Create database with "Complete reference data (with flows)" template
3. File → Import → Other → EcoSpold 2
4. Select your .spold files or .zip archive

## Important Notes

⚠️ **License Requirement:** You must have a valid ecoinvent license to use EcoSpold2 data. This tool only provides the import capability, not the data itself.

⚠️ **Unofficial Build:** This is not affiliated with GreenDelta GmbH or the official openLCA project.

⚠️ **Why This Exists:** The official openLCA releases disabled EcoSpold2 import to promote pre-converted database sales. This fork restores the functionality for users who prefer to import their licensed data directly.

## Technical Details

- Based on: openLCA 2.5.1-SNAPSHOT
- Java: 21+
- Dependencies: olca-modules 2.5.1-SNAPSHOT
- Import library: olca-ecospold-2 2.1.0

## Support

- Issues: https://github.com/ankur-paan/olca-app/issues
- EcoSpold2 data questions: Contact ecoinvent support
- Official openLCA support: https://www.openlca.org/
```

### Option B: Source-Only Distribution

If you don't want to build binaries:

```bash
# Just push the source code
git add .gitignore README.md ECOSPOLD2_IMPORT_GUIDE.md olca-app/
git commit -m "Enable EcoSpold2 import functionality"
git push origin master
```

Users build it themselves following the README.

### Option C: Contribute to Official Repository

Create a Pull Request to GreenDelta/olca-app:

**PR Title:** "Optional EcoSpold2 Import Feature (User-Licensed Data)"

**PR Description:**
- Propose re-enabling as optional feature for users with ecoinvent licenses
- Emphasize educational/research benefits
- Include comprehensive documentation
- Offer to maintain the feature

## What NOT to Include

❌ **Never commit these:**
- `sample/` folder (ecoinvent data)
- Any `.spold` files
- `ecospold2_flow_mapping.csv` with ecoinvent UUIDs
- Database files (`.zolca`)
- Built binaries with bundled data

✅ **Safe to include:**
- Python script (generic tool)
- Documentation
- Modified source code
- Empty template files

## Post-Publishing

### If providing binaries:

1. **Build clean packages:**
```bash
python prepare-release.py
# Export via Eclipse PDE
cd olca-app-build
python -m package
```

2. **Upload to GitHub Release:**
   - `openLCA_win_2.5.1-ecospold2.zip`
   - `openLCA_linux_2.5.1-ecospold2.tar.gz`
   - `openLCA_mac_2.5.1-ecospold2.dmg`

3. **Add checksums:**
```bash
# Windows
Get-FileHash openLCA_win_2.5.1-ecospold2.zip -Algorithm SHA256
```

### If source-only:

Update README with:
- Link to ECOSPOLD2_IMPORT_GUIDE.md
- Build instructions
- System requirements
- Contact information

## Maintenance Plan

- Monitor issues for bugs
- Keep in sync with upstream openLCA releases
- Update documentation as needed
- Respond to user questions about EcoSpold2 import

## Legal Checklist

- [ ] No proprietary ecoinvent data in repository
- [ ] Clear license compliance warnings in documentation
- [ ] Fork attribution to GreenDelta (original authors)
- [ ] Mozilla Public License 2.0 maintained (openLCA license)
- [ ] No trademark violations (don't claim official status)

---

## Current Status

- [x] Code changes complete
- [x] Documentation created
- [x] `.gitignore` configured
- [ ] **Ready to commit and push**
- [ ] Create GitHub release
- [ ] Build distribution packages (optional)
