# Tycho Maven Build Configuration

This directory contains the Tycho Maven build configuration for building openLCA using Maven instead of the Eclipse PDE Export wizard.

## Structure

- `pom.xml` - Parent POM with Tycho plugin management
- `product/pom.xml` - Product build module that creates the distributable products

## Prerequisites

1. **Maven 3.6+** installed and configured
2. **Java 21** (as specified in the product configuration)
3. **Local libs directory**: The `platform.target` file references a local directory at `/Users/tomasbaronas/Documents/libs`. This directory must exist and contain the required JAR files for the build to succeed.

   For CI/CD environments, you have two options:
   - **Option 1**: Convert the local directory to a p2 repository and update `platform.target` to reference it
   - **Option 2**: Ensure the libs directory is available at the expected path (or update the path in `platform.target`)

## Building

### Build the plugin and product

From the `olca-app-build` directory:

```bash
mvn clean verify
```

This will:
1. Build the `olca-app` plugin using Tycho
2. Create a p2 repository with the product
3. Materialize the product for all configured platforms (Linux, macOS, Windows)
4. Create archives (tar.gz for Linux/macOS, zip for Windows)
5. Output the products to `olca-app-build/build/` directory

### Build only the plugin

From the `olca-app` directory:

```bash
mvn clean package
```

This builds only the plugin and copies Maven dependencies to the `libs` folder.

## Output Structure

After a successful build, the products will be in:
- `olca-app-build/build/linux.gtk.x86_64/` - Linux product
- `olca-app-build/build/macosx.cocoa.x86_64/` - macOS product  
- `olca-app-build/build/win32.win32.x86_64/` - Windows product

These directories match the structure expected by the Python packaging script (`olca-app-build/package/main.py`).

## Integration with Existing Packaging Script

The Tycho build outputs products to the same directory structure as the Eclipse PDE Export, so your existing Python packaging script should work without modification:

```bash
cd olca-app-build
python -m package
```

## Local Directory Dependency

The `platform.target` file includes a local directory dependency:
```xml
<location path="/Users/tomasbaronas/Documents/libs" type="Directory"/>
```

For CI/CD environments, you may need to:

1. **Convert to p2 repository**: Use Eclipse's p2 publisher to convert the directory to a p2 repository
2. **Use environment variable**: Modify the target platform to use an environment variable or Maven property
3. **Copy to build environment**: Ensure the libs directory is available at the expected path

## GitHub Actions Example

Here's an example GitHub Actions workflow:

```yaml
name: Build with Tycho

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      - name: Build with Tycho
        run: |
          cd olca-app-build
          mvn clean verify
      - name: Package distributions
        run: |
          cd olca-app-build
          python -m package
```

## Troubleshooting

### Target platform resolution fails

If you see errors about missing dependencies:
- Ensure the local libs directory exists and contains the required JARs
- Check that all p2 repository URLs in `platform.target` are accessible
- Verify the target platform file path is correct

### Product not found

If Tycho can't find the product:
- Ensure `openLCA.product` exists in `olca-app/` directory
- Check that the product ID in the product file matches the configuration in `product/pom.xml`

### Build outputs to wrong location

The output directory is configured in `product/pom.xml` in the `tycho-p2-director-plugin` configuration. Adjust the `outputDirectory` if needed.

