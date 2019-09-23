# olca-app-html
This folder contains the sources of the HTML and JavaScript files that are
packaged with openLCA.

## Build
You need to have Node.js with npm installed. With this, navigate to the
`olca-app-html` folder and install the required modules:

```bash
cd olca-app-html
npm install
```

This also installs a local version of `webpack` with which you can build the
distribution package:

```bash
npx webpack
```

The output is generated in the `dist` folder of this directory and packaged
into a zip file that is copied to the `../olca-app/html` folder.
