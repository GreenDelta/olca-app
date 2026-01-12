# Tycho Maven Build Configuration

This directory contains the Tycho Maven build configuration for building openLCA using Maven instead of the Eclipse PDE Export wizard.

## Structure

- `pom.xml` - Parent POM with Tycho plugin management and version properties
- `product/pom.xml` - Product build module that creates the distributable products
- `product/openLCA.product` - Copy of `../../olca-app/openLCA.product` (automatically synchronized during build)

## Why This Structure?

### Three POM Files Required

Tycho requires this multi-module structure because:

1. **Parent POM** (`olca-app-build/pom.xml`):
   - Packaging: `pom`
   - Role: Aggregator and plugin management
   - Contains: Tycho version, common properties, pluginManagement

2. **Plugin POM** (`olca-app/pom.xml`):
   - Packaging: `eclipse-plugin`
   - Role: Builds the OSGi bundle/plugin
   - Cannot be combined with eclipse-repository packaging

3. **Product POM** (`product/pom.xml`):
   - Packaging: `eclipse-repository`
   - Role: Materializes and archives distributable products
   - Cannot be combined with eclipse-plugin packaging

**Why can't we have just one POM?** Maven enforces a one-to-one relationship between a module and its packaging type. You cannot have a single POM with both `eclipse-plugin` and `eclipse-repository` packaging.

### Product File Synchronization

The `openLCA.product` file is copied from `olca-app/openLCA.product` during the build:
- Single source of truth in the plugin project
- Product module gets a copy via maven-resources-plugin
- Automatically synchronized during `mvn clean verify`

## Prerequisites

1. **Maven 3.6+** installed and configured
2. **Java 21** (as specified in the product configuration)
3. **olca-modules built**: The olca-app plugin depends on olca-core, olca-io, etc. Run `mvn install` in olca-modules first
4. **Target platform**: The `platform.target` file in `olca-app/` must be properly configured

## Building

### Build Everything (Plugin + Product)

From the `olca-app-build` directory:

```bash
mvn clean verify
```

This will:
1. Build the `olca-app` plugin using Tycho
2. Create a p2 repository with the product
3. Materialize the product for all configured platforms (Linux x64, macOS x64, macOS ARM64, Windows x64)
4. Create archives (tar.gz for Linux/macOS, zip for Windows)
5. Output the products to `olca-app-build/build/` directory

### Build Only the Plugin

From the `olca-app` directory:

```bash
mvn clean package
```

This builds only the plugin and copies Maven dependencies to the `libs` folder (useful during development).

## Output Structure

After a successful build, the products will be in:
- `olca-app-build/build/linux.gtk.x86_64/` - Linux product
- `olca-app-build/build/macosx.cocoa.x86_64/` - macOS Intel product
- `olca-app-build/build/macosx.cocoa.aarch64/` - macOS ARM product
- `olca-app-build/build/win32.win32.x86_64/` - Windows product

These directories match the structure expected by the Python packaging script.

## Integration with Existing Packaging Script

The Tycho build outputs products to the same directory structure as the Eclipse PDE Export, so your existing Python packaging script should work without modification:

```bash
cd olca-app-build
python -m package
```

## Understanding Tycho Packaging Types

### `eclipse-plugin` (olca-app module)
- **Purpose**: Build a single OSGi bundle/plugin
- **Input**: Java source code, `META-INF/MANIFEST.MF`, `build.properties`
- **Output**: A plugin JAR file with all classes and resources
- **Example**: The olca-app plugin with all your application code

### `eclipse-repository` (product module)
- **Purpose**: Build complete Eclipse/RCP applications for distribution
- **Input**: `.product` file, target platform definition
- **Output**:
  - P2 repository (Eclipse provisioning metadata)
  - Materialized products (complete application directories for each OS)
  - Archives (tar.gz/zip files ready for distribution)
- **Example**: The final openLCA application users download

### Why Both Are Needed

Think of it like building a car:
- `eclipse-plugin` = Building individual parts (engine, wheels, chassis)
- `eclipse-repository` = Assembling all parts into a complete, drivable car

## Troubleshooting

### Target platform resolution fails

If you see errors about missing dependencies:
- Ensure all p2 repository URLs in `platform.target` are accessible
- Check that the target platform file path is correct in the POMs
- Verify internet connection (if using remote p2 repositories)

### Missing olca-* dependencies

If you see errors about missing olca-core, olca-io, etc.:
```bash
cd ../../olca-modules
mvn clean install
```

### Product not found

If Tycho can't find the product:
- Ensure the file `product/openLCA.product` exists (it's copied during the build from `olca-app/openLCA.product`)
- Check that the product ID in the product file matches the configuration in `product/pom.xml` (should be `olca-app.openLCA`)
- Try running `mvn clean verify` to ensure the file gets copied fresh

### Build outputs to wrong location

The output directory is configured in `product/pom.xml` in the `tycho-p2-director-plugin` configuration. The destination is set to `${project.basedir}/../build` which resolves to `olca-app-build/build/`.

## Notes for CI/CD

When setting up CI/CD pipelines:
1. First build and install olca-modules: `cd olca-modules && mvn clean install`
2. Then build olca-app: `cd olca-app/olca-app-build && mvn clean verify`
3. Use the archived products from `olca-app-build/build/` for distribution
