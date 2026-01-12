# Tycho Build Implementation - Changes Summary

This document describes the changes made to implement Maven/Tycho-based builds for openLCA.

## Files Created

### 1. `/olca-app-build/pom.xml`
**Purpose**: Parent aggregator POM with plugin management
- **Packaging**: `pom`
- **Role**: 
  - Aggregates both `olca-app` and `product` modules
  - Defines Tycho version (4.0.7) and common properties
  - Provides `<pluginManagement>` for Tycho plugins (inherited by child modules)
  - Configures target platform and execution environment (Java 21)
  - Defines build environments (Linux, macOS Intel, macOS ARM, Windows)

### 2. `/olca-app-build/product/pom.xml`
**Purpose**: Eclipse repository builder for creating distributable products
- **Packaging**: `eclipse-repository`
- **Role**:
  - Uses `tycho-p2-director-plugin` to materialize and archive products
  - Creates distributable products for all platforms
  - Outputs to `olca-app-build/build/` directory
  - Generates tar.gz archives for Linux/macOS, zip for Windows

### 3. `/olca-app-build/product/openLCA.product` (symlink)
**Purpose**: Links to the single source of truth for product definition
- **Symlink target**: `../../olca-app/openLCA.product`
- **Rationale**: Avoids duplication while allowing the repository module to reference the product

### 4. `/olca-app-build/product/.gitignore`
**Content**: `target/`
**Purpose**: Prevents Maven build artifacts from being committed

### 5. `/olca-app-build/TYCHO_BUILD.md`
**Purpose**: Comprehensive documentation explaining:
- Why three POMs are required (architectural constraint)
- The role of each packaging type (`eclipse-plugin` vs `eclipse-repository`)
- Build instructions
- Troubleshooting guide
- CI/CD integration notes

## Files Modified

### `/olca-app/olca-app/pom.xml`
**Changes**:
- **Packaging changed**: From `pom` to `eclipse-plugin`
- **Parent added**: References `olca-app-build` as parent
- **Tycho plugins added**:
  - `tycho-maven-plugin` - Core Tycho support
  - `tycho-packaging-plugin` - Eclipse plugin packaging
  - `target-platform-configuration` - Platform and environment configuration
- **Dependency management**:
  - All dependencies marked with `<scope>runtime</scope>`
  - Used only for copying JARs to `libs/` folder via `maven-dependency-plugin`
  - Not used for Tycho dependency resolution (uses target platform instead)

## Structure Overview

```
olca-app-build/
├── pom.xml                    [NEW] Parent aggregator (packaging=pom)
├── TYCHO_BUILD.md             [NEW] Documentation
├── CHANGES.md                 [NEW] This file
└── product/
    ├── pom.xml                [NEW] Repository builder (packaging=eclipse-repository)
    ├── .gitignore             [NEW] Ignores target/
    └── openLCA.product        [NEW] Symlink to ../../olca-app/openLCA.product

olca-app/
└── olca-app/
    ├── pom.xml                [MODIFIED] Now uses Tycho (packaging=eclipse-plugin)
    ├── openLCA.product        [EXISTING] Single source of truth
    ├── platform.target        [EXISTING] Target platform definition
    └── META-INF/MANIFEST.MF   [EXISTING] OSGi bundle manifest
```

## Why This Structure?

### Three POMs Required

Maven/Tycho enforces a strict separation:

1. **Aggregator POM** (`pom` packaging)
   - Cannot build artifacts itself
   - Only coordinates child modules
   - Provides shared configuration via `<pluginManagement>`

2. **Plugin POM** (`eclipse-plugin` packaging)
   - Builds OSGi bundles
   - Cannot use product-building plugins

3. **Repository POM** (`eclipse-repository` packaging)
   - Builds distributable products
   - Cannot build plugins directly

**This is NOT a configuration choice** - it's a fundamental Maven/Tycho architectural requirement. You cannot merge these into fewer files.

### Single Product File via Symlink

The `openLCA.product` file:
- Lives in `olca-app/` (for Eclipse IDE development)
- Included in plugin's `build.properties` (packaged with the bundle)
- Symlinked from `product/` (for Tycho repository build)
- **Result**: One source of truth, no duplication

## Building

### Full build (plugin + products):
```bash
cd olca-app-build
mvn clean verify
```

### Plugin only:
```bash
cd olca-app
mvn clean package
```

## Output

Products are materialized to:
- `olca-app-build/build/linux.gtk.x86_64/`
- `olca-app-build/build/macosx.cocoa.x86_64/`
- `olca-app-build/build/macosx.cocoa.aarch64/`
- `olca-app-build/build/win32.win32.x86_64/`

Archives created in `olca-app-build/build/products/`:
- Linux: `.tar.gz`
- macOS: `.tar.gz`
- Windows: `.zip`

## Integration with Existing Workflow

The Python packaging script (`olca-app-build/package/`) should work without modification as the Tycho build outputs to the same directory structure as the Eclipse PDE Export.

## Prerequisites

1. **Java 21** (set `JAVA_HOME`)
2. **Maven 3.6+**
3. **olca-modules built first**:
   ```bash
   cd olca-modules
   mvn clean install
   ```

