## Building the distribution packages

### PDE Export

To build the distribution packages, we currently use the standard PDE Export
wizard. Click on the `olca-app` project and then on `Export...` from the context
menu. Select `Plug-in Development > Eclipse Product` from the export wizard and
select the following options in the export dialog:

* Configuration: `/olca-app/openLCA.product` (should be the default)
* Root directory: `openLCA`
* Synchronize before exporting: yes [x]
* Destination directory: choose the `olca-app-build/build` folder of this project
* Generate p2 repository: no [ ] (would be just overhead)
* Export for multiple platforms: yes [x]
* (take the defaults for the others)

In the next page, select the platforms for which you want to build the product.
After the export, you need to run the package module `package` to copy
resources like the Java runtime, the native math libraries, etc. to the
application folder and to create the installers.

The packager script can build distribution packages for the following platforms
(but you do not need to build them all, if a platform product is missing it is
simply ignored in the package script):

* Linux gtk x86_64
* macOS cocoa x86_64
* Windows win32 x86_64

The packager script may download build tools (7zip, NSIS on Windows), the JRE,
and native libraries if these are missing.

-------------

### Tycho Maven Build

As an alternative to the Eclipse PDE Export wizard, you can build the distribution packages using **Maven with Tycho**.

#### Prerequisites

1. **Java 21** installed and `JAVA_HOME` set
2. **Maven 3.9+** installed
3. **olca-modules** built and installed:
   cd olca-modules
   mvn clean install
   #### Build Command

To build the product for all platforms:

cd olca-app-build
mvn clean verify

This command builds the `olca-app` plugin and creates distributable products for all platforms (Linux, macOS Intel, macOS ARM, Windows) in the `olca-app-build/build/` directory.

**Note:** For release builds without the SNAPSHOT qualifier, add `-Prelease` to the build command.

The Tycho build outputs to the same directory structure as the PDE Export, so the Python packaging script works unchanged.

#### Multi-module Structure

The Tycho build uses three Maven POMs:
- `olca-app-build/pom.xml` - Parent aggregator with shared configuration
- `olca-app/pom.xml` - Builds the OSGi plugin bundle  
- `olca-app-build/product/pom.xml` - Creates distributable products

**Note**: You can use either the PDE Export wizard or `mvn clean verify` to create the distribution packages.

-------------

### Steps when building a release package

#### Export and package the release (Win, Linux, macOS)

1. Check that the `olca-app` and `olca-modules` repositories are on the master
   branch and are in sync with our GitHub repository.

2. Update the property files with the latest translations (cf. [Update the translations](#update-the-translations)). Make sure to check
   the relevance (case, punctuation, ...) and that no translations has been removed.

3. Copy the welcome page background image into
   `olca-app/olca-app-html/images/background_start_page.jpg`.
   This image is not stored on GitHub as it is often modified and quite large.

4. Run the `prepare-release.py` script that updates the olca-modules libraries
   in the olca-app, creates fresh database templates, updates the html-pages,
   creates the Jython bindings from the current modules, etc.:

   ```bash
   cd olca-app
   python prepare-release.py
   ```

5. Run the PDE export as described above ([PDE Export](#pde-export)) or use the Tycho Maven build ([Tycho Maven](#tycho-maven-build)).

6. Run the packaging module `package`.

   ```bash
   cd olca-app-build
   python -m package
   ```

   Note: To also create the Windows installer, add `--winstaller` to the
   command and to package _MKL_ native library instead of _BLAS_, add  `--mkl`.

-------------

#### Independent distribution
Prepare the independent distribution of the Mac app (`.dmg`) (only on macOS).

In order to pass the Gatekeeper protection, the Mac bundle freshly packaged has
to be signed in depth with an Apple Development certificate, notarized and
stapled. When testing, one should make a test copy after running the script.

Prerequisites:
 * __Xcode__ (version > 13),
 * __Keychain Access__,
 * [create-dmg](https://github.com/create-dmg/create-dmg) (version > 1.1.0):
   `brew install create-dmg`,

1. Set up the signing certificate:
   * In __Xcode__, Open `Settings > Accounts > +` and sign in with the Green Delta Apple ID.
   * Add the _Developer ID Application Certificate_ in `Manage Certificates...`.
   * Get the name of the certificate:
      * Open __Keychain Access__,
      * In `login > My Certificates` the full name is `Developer ID
        Application: GreenDelta GmbH (<code>)`.

2. Set up the `notarytool`credentials:
   * Go to https://appstoreconnect.apple.com/access/users and sign in with the
     Green Delta Apple ID.
   * Select the _Keys_ tab and create a key named `notarytool` with _Developer_
     role.
   * Download the key API with caution (it can be downloaded only one time).
   * Add the password to your system:
     * Run the following command `xcrun notarytool store-credentials`:
     * Profile name: `notarytool`,
     * Path to...: `path/to/the/API/key/AuthKey_***********.p8`,
     * App Store Connect API Key ID: see key file name or key ID on
       https://appstoreconnect.apple.com,
     * App Store Connect API Issuer ID: see Issuer ID on
       https://appstoreconnect.apple.com.

3. Run the following command to sign, notarized, staple the app and embellish
  the disk image:

    ```bash
    cd olca-app-build
    ./mac_dist.sh --dev-id-app <certificate full name> dmg
    ```

-------------

####  App Store Distribution
Prepare the App store distribution of the Mac app (`.pkg`) (only on macOS)

Prerequisites:
* __Xcode__ (version > 13),
* __Keychain Access__.

1. Set up the signing certificates:
    * In __Xcode__, open `Settings > Accounts > +` and sign in with the Green Delta Apple ID.
    * Add the _Developer ID Application Certificate_ in
      `Manage Certificates...`.
    * Get the name of the certificate:
        * Open __Keychain Access__,
        * In `login > My Certificates` the full name are
          `3rd Party Mac Developer Installer: GreenDelta GmbH (<code>)` and
          `3rd Party Mac Developer Application: GreenDelta GmbH (<code>)`.

2. Set up the `notarytool`credentials:
    * See the second section of
      [independent distribution](#independent-distribution).

3. Set up the `altool`credentials:
    * Go to https://appleid.apple.com/ and sign in with the Green Delta Apple
      ID.
    * Select Application password and create a new password named
      `altool` and copy it.
    * Add the password to your system:
        * Open __Keychain Access__ and select _File > New password Item..._,
        * Fill the form with the password name `altool`, your Apple ID and the
          password you have copied.

4. Run the following command to sign, validate and upload the app to the Apple
   Store:

    ```bash
    cd olca-app-build
    ./mac_dist.sh \
      --store-id-app <application certificate full name> \
      --store-id-installer <installer certificate full name> \
      --user <Apple user ID> pkg
    ```

#### Test the apps!

1. Open every package, open a database and eventually a product system.
2. Test the DMG on a different computer than the one used to sign and notarize.

-------------

### Create icons for the distribution

#### macOS

In order to generate the `.icns` of the macOS distribution, create a 1024x1024 PNG image and use the script from `olca-app-build/create_icns.sh`:
```bash
create_icns.sh logo.png
```

#### Windows

Multiple steps are necessary to combine the different icons into a `.ico` file:

1. Create 16x16, 32x32, 48x48 and 256x256 PNG icons with an alpha layer (transparency) (in 32 bit).
2. [conversion into 8 bit BMP] In GIMP, convert the 16x16, 32x32 and 48x48 icons into 8 bit BMP by removing the alpha layer (transparency) and using the _indexed_ mode.
3. [conversion into 32 bit BMP] In GIMP again, convert the 16x16, 32x32, 48x48 and 256x256 PNG icons into BMP while keeping the alpha layer and use the _RGB_ mode.
4. [conversion to Windows `.ico`] Use a converter or the following macOS command to pack the 7 icons into one ICO file.

```bash
convert 16_8bit.bmp 16_32bit.bmp 32_8bit.bmp 32_32bit.bmp 48_8bit.bmp 48_32bit.bmp 256_32bit.bmp logo.ico
```

### Update the translations

openLCA users can update the translation of the application by using the 
[translate.openlca.org](https://translate.openlca.org/) website. For each 
release, the translations are updated with the following steps:

1. Download the latest translations from the `translate.openlca.org` website.
   One can check wether the result of the _Edited_ filter is populated and 
   download the file accordingly.

2. Copy the translations into the `olca-app/OSGI-INF/l10n` and `olca-app/src/
   org/openlca/app` folders and check if no translations has disapeared and
   that the case and punctuations are correct.

3. Commit the new translations into the `master` branch.

4. Update `translate.openlca.org` server by replacing the property files in
   `/opt/translator/data/original/` with the new ones and removing the change
   files in `/opt/translator/data/olca-app` and `/opt/translator/data/olca-app-osgi`. Before doing it, best practice is to make a backup of the `/opt/translator/data` named `/opt/translator/data.backup`.