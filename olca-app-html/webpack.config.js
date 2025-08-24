const CopyPlugin = require('copy-webpack-plugin');
const ZipPlugin = require('zip-webpack-plugin');
const path = require('path');
const dist = path.resolve(__dirname, 'dist');

const config = {

  entry: {
    home: './src/home/home.tsx',
    report: './src/report/report.tsx',
    python: './src/python/python.tsx',
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
    new CopyPlugin({
      patterns: [
        { from: 'src/**/*.html', to: () => `${dist}/[name][ext]` },
        { from: 'src/**/*.css', to: () => `${dist}/[name][ext]` },
        { from: 'images', to: dist + "/images" },
        { from: 'fonts', to: dist + "/fonts" },
        { from: 'node_modules/react/umd/react.production.min.js', to: dist + '/lib/react.js' },
        { from: 'node_modules/react-dom/umd/react-dom.production.min.js', to: dist + '/lib/react-dom.js' },
        { from: 'node_modules/chart.js/dist/chart.min.js', to: dist + '/lib/chart.min.js' },
        { from: 'node_modules/milligram/dist/milligram.min.css', to: dist + '/lib/milligram.min.css' },
        { from: 'node_modules/normalize.css/normalize.css', to: dist + '/lib/normalize.css' },
        { from: 'node_modules/jquery/dist/jquery.min.js', to: dist + '/lib/jquery.min.js' },
        { from: 'node_modules/@picocss/pico/css/pico.min.css', to: `${dist}/lib/pico.min.css` },
      ]
    }),
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
  },
};

module.exports = (_env, argv) => {

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
