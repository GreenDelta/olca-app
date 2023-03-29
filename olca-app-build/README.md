## Building the distribution packages
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
After the export, you need to run the package script `package.py` to copy
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


### Steps when building a release package

1. Check that the `olca-app` and `olca-modules` repositories are on the master
   branch and are in sync with our Github repository

2. Run the `prepare-release.py` script that updates the olca-modules libraries
   in the olca-app, creates fresh database templates, updates the html-pages,
   creates the Jython bindings from the current modules, etc:

```bash
cd olca-app
python prepare-release.py
```

5. Run the PDE export as described above

6. Run the packaging script `package.py`

```bash
cd olca-app-build
python package.py
```

7. Test it

#### Sign the Mac app

In order to pass the Gatekeeper protection the Mac app is signed with an Apple
Development certificate on macOS.

* Add Green Delta signing certificate to __Xcode__:
  * Open `Settings > Accounts > +` and sign in with Green Delta Apple ID.
  * Add the developer certificate in `Manage Certificates...`.
* Copy the name of the certificate in __Keychain Access__. In 
`login > My Certificates` copy the full name of the _Ap


