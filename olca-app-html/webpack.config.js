const CopyPlugin = require('copy-webpack-plugin');
const ZipPlugin = require('zip-webpack-plugin');
const webpack = require('webpack');
const path = require('path');
const fs = require('fs');
const os = require('os');
const dist = path.resolve(__dirname, 'dist');

// Custom plugin to clean workspace HTML folder
class CleanWorkspaceHtmlPlugin {
  apply(compiler) {
    compiler.hooks.beforeRun.tap('CleanWorkspaceHtmlPlugin', () => {
      const workspaceHtmlDir = path.join(os.homedir(), 'openLCA-data-1.4', 'html', 'olca-app');
      
      if (fs.existsSync(workspaceHtmlDir)) {
        console.log('🧹 Cleaning workspace HTML folder:', workspaceHtmlDir);
        fs.rmSync(workspaceHtmlDir, { recursive: true, force: true });
        console.log('✅ Workspace HTML folder cleaned successfully');
      } else {
        console.log('ℹ️  Workspace HTML folder does not exist, skipping cleanup');
      }
    });
  }
}

const config = {

  entry: {
    home: './src/home/home.tsx',
    report: './src/report/report.tsx',
    agent: './src/agent/agent.tsx',
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.css$/,
        use: [
          'style-loader',
          'css-loader',
          'postcss-loader'
        ],
      },
    ]
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
    alias: {
      '@': path.resolve(__dirname, 'src/agent'),
      '@/components': path.resolve(__dirname, 'src/agent/components'),
      '@/hooks': path.resolve(__dirname, 'src/agent/hooks'),
      '@/lib': path.resolve(__dirname, 'src/agent/lib'),
      '@/providers': path.resolve(__dirname, 'src/agent/providers')
    }
  },
  output: {
    filename: '[name].js',
    path: dist,
  },
  plugins: [
    new CleanWorkspaceHtmlPlugin(),
    new webpack.DefinePlugin({
      'process.env.REACT_APP_LANGGRAPH_URL': JSON.stringify(process.env.REACT_APP_LANGGRAPH_URL || 'http://localhost:8000'),
      'process.env.REACT_APP_LANGGRAPH_API_KEY': JSON.stringify(process.env.REACT_APP_LANGGRAPH_API_KEY || ''),
    }),
    new CopyPlugin({
      patterns: [
        { from: 'src/**/*.html', to: () => `${dist}/[name][ext]` },
        { from: 'images', to: dist + "/images" },
        { from: 'fonts', to: dist + "/fonts" },
        { from: 'node_modules/react/umd/react.production.min.js', to: dist + '/lib/react.js' },
        { from: 'node_modules/react-dom/umd/react-dom.production.min.js', to: dist + '/lib/react-dom.js' },
        { from: 'node_modules/chart.js/dist/chart.min.js', to: dist + '/lib/chart.min.js' },
        { from: 'node_modules/milligram/dist/milligram.min.css', to: dist + '/lib/milligram.min.css' },
        { from: 'node_modules/normalize.css/normalize.css', to: dist + '/lib/normalize.css' },
        { from: 'node_modules/codemirror/lib/*.*', to: () => `${dist}/lib/[name][ext]` },
        { from: 'node_modules/codemirror/theme/ayu-mirage.css', to: () => `${dist}/lib/ayu-mirage.css` },
        { from: 'node_modules/codemirror/mode/python/python.js', to: dist + '/lib/python.js' },
        { from: 'node_modules/jquery/dist/jquery.min.js', to: dist + '/lib/jquery.min.js' },
        { from: 'node_modules/@picocss/pico/css/pico.min.css', to: `${dist}/lib/pico.min.css` },
        { from: 'node_modules/tw-animate-css/dist/tw-animate.css', to: `${dist}/tw-animate-css` },
      ]
    }),
  ],
  // When importing a module whose path matches one of the following, just
  // assume a corresponding global variable exists and use that instead.
  // This is important because it allows us to avoid bundling all of our
  // dependencies, which allows browsers to cache those libraries between builds.
  externals: {
    "react": "React",
    "react-dom": "ReactDOM",
    "chart.js": "Chart",
    "codemirror": "CodeMirror",
  },
};

module.exports = (_env, argv) => {

  if (argv.mode === 'development') {
    config.devtool = 'source-map';
    
    // Dev server configuration
    config.devServer = {
      static: {
        directory: path.join(__dirname, 'dist'),
      },
      port: 3000,
      hot: true,
      liveReload: true,
      open: true,
      historyApiFallback: true,
      compress: true,
      client: {
        overlay: {
          errors: true,
          warnings: false,
        },
      },
      // Proxy API requests to your LangGraph server
      proxy: [
        {
          context: ['/api'],
          target: process.env.REACT_APP_LANGGRAPH_URL || 'http://localhost:8000',
          changeOrigin: true,
          secure: false,
        },
      ],
    };
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
