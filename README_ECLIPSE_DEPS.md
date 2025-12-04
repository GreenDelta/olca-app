# How to Add Eclipse Dependencies for VS Code/Cursor

## The Problem
This project uses Eclipse RCP, which means it needs Eclipse JAR files (like `org.eclipse.swt`, `org.eclipse.ui`, etc.). VS Code/Cursor doesn't automatically know where to find these.

## The Solution (2 Steps)

### Step 1: Get the Eclipse JAR files

You need to copy Eclipse JAR files into `olca-app/libs/`. Here's the easiest way:

**Option A: Use Eclipse IDE (Recommended)**
1. Install [Eclipse IDE for RCP and RAP Developers](https://www.eclipse.org/downloads/packages/)
2. Open Eclipse and import this project
3. Open `olca-app/platform.target` and click "Set as Target Platform" (downloads all bundles)
4. Copy JARs from `~/.p2/pool/plugins/` to `olca-app/libs/`
   - Look for files like `org.eclipse.ui_*.jar`, `org.eclipse.swt_*.jar`, etc.

**Option B: Download Eclipse Platform SDK**
1. Download from https://download.eclipse.org/eclipse/downloads/
2. Extract and copy JARs from `eclipse/plugins/` to `olca-app/libs/`

### Step 2: Run the script

```bash
python3 update-classpath.py
```

This script:
- Scans `olca-app/libs/` for JAR files
- Adds them to `olca-app/.classpath` 
- VS Code/Cursor reads `.classpath` to know what's available

Then refresh your IDE and the import errors should be gone!

## What the script does

The `.classpath` file tells Java IDEs where to find dependencies. The script automatically adds entries like:

```xml
<classpathentry kind="lib" path="libs/org.eclipse.ui_3.136.0.jar"/>
```

When VS Code/Cursor sees this, it knows to look in `libs/org.eclipse.ui_3.136.0.jar` for classes.

## Required Bundles

Based on `olca-app/META-INF/MANIFEST.MF`, you need:
- `org.eclipse.ui`
- `org.eclipse.core.runtime`
- `org.eclipse.swt` (platform-specific - for macOS ARM: `org.eclipse.swt.cocoa.macosx.aarch64`)
- `org.eclipse.jface`
- `org.eclipse.ui.forms`
- `org.eclipse.ui.views`
- `org.eclipse.draw2d`
- `org.eclipse.gef`
- And others listed in the Require-Bundle section

## That's it!

Just copy JARs → run script → refresh IDE. Simple!




