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

This also installs a local version of `webpack` which is used to create the
distribution package. The build of this package can be invoked via:

```bash
npm run build
```

The output is generated in the `dist` folder of this directory and packaged
into a zip file that is copied to the `../olca-app/html` folder.

You can also run `webpack` in watch mode for development via:

```bash
npm run dev
```

This will also create source maps for debugging.
