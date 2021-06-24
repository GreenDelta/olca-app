const CopyPlugin = require('copy-webpack-plugin');
const ZipPlugin = require('zip-webpack-plugin');
const path = require('path');
const dist = path.resolve(__dirname, 'dist');

const config = {

    entry: {
        home: './src/home/home.tsx',
        report: './src/report/report.tsx',
        kml_results: './src/kml_results/kml_results.ts',
        kml_editor: './src/kml_editor/kml_editor.tsx',
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/,
            },
        ]
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js']
    },
    output: {
        filename: '[name].js',
        path: dist,
    },
    plugins: [
        new CopyPlugin([
            { from: 'src/**/*.html', to: dist, flatten: true },
            { from: 'src/**/*.css', to: dist, flatten: true },
            { from: 'images', to: dist + "/images" },
            { from: 'fonts', to: dist + "/fonts" },
            { from: 'node_modules/react/umd/react.production.min.js', to: dist + '/lib/react.js', type: 'file' },
            { from: 'node_modules/react-dom/umd/react-dom.production.min.js', to: dist + '/lib/react-dom.js', type: 'file' },
            { from: 'node_modules/chart.js/dist/chart.min.js', to: dist + '/lib/chart.min.js', type: 'file' },
            { from: 'node_modules/milligram/dist/milligram.min.css', to: dist + '/lib/milligram.min.css', type: 'file' },
            { from: 'node_modules/normalize.css/normalize.css', to: dist + '/lib/normalize.css', type: 'file' },
            { from: 'node_modules/codemirror/lib/*.*', to: dist + '/lib', flatten: true },
            { from: 'node_modules/codemirror/mode/python/python.js', to: dist + '/lib/python.js', type: 'file' },
            { from: 'node_modules/codemirror/mode/xml/xml.js', to: dist + '/lib/xml.js', type: 'file' },
            { from: 'node_modules/ol/ol.css', to: dist + '/lib/ol.css', type: 'file' },
            { from: 'node_modules/jquery/dist/jquery.min.js', to: dist + '/lib/jquery.min.js', type: 'file' },
            { from: 'node_modules/leaflet/dist/leaflet.css', to: dist + '/lib/leaflet.css', type: 'file' },
            { from: 'node_modules/leaflet/dist/leaflet.js', to: dist + '/lib/leaflet.js', type: 'file' },
            { from: 'node_modules/leaflet/dist/images/*.png', to: dist + '/lib/images', flatten: true },
            { from: 'node_modules/heatmap.js/build/heatmap.min.js', to: dist + '/lib/heatmap.min.js', type: 'file' },
            { from: 'node_modules/heatmap.js/plugins/leaflet-heatmap/leaflet-heatmap.js', to: dist + '/lib/leaflet-heatmap.js', type: 'file' },
        ]),
    ],
    resolve: {
        // Add `.ts` and `.tsx` as a resolvable extension.
        extensions: [".ts", ".tsx", ".js"]
    },
    // When importing a module whose path matches one of the following, just
    // assume a corresponding global variable exists and use that instead.
    // This is important because it allows us to avoid bundling all of our
    // dependencies, which allows browsers to cache those libraries between builds.
    externals: {
        "react": "React",
        "react-dom": "ReactDOM",
        "chart.js": "Chart",
        "codemirror": "CodeMirror",

        // when OpenLayers would come with a distribution library
        // in the npm package, we could map these prefixes... and
        // the build would be probably faster
        // "ol": "ol",
        // "ol/format": "ol.format",
        // "ol/layer": "ol.layer",
        // "ol/source": "ol.source",
        // "ol/style": "ol.style",

    },
};

module.exports = (env, argv) => {

    if (argv.mode === 'development') {
        config.devtool = 'source-map';
    }

    if (argv.mode === 'production') {
        config.plugins.push(new ZipPlugin({
            path: path.resolve(__dirname, '../olca-app/html'),
            filename: "base_html.zip",
            exclude: [/\.map$/],
        }))
    }

    return config;
};
