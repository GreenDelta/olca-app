/**
 * Description:
 * ------------
 * This scripts prepares the cache for fonts that are not available directly in our repository
 * This script is supposed to run before the build (production, development) of the home page so that fonts are correctly put in place for the build to inject them in final output.
 * The respective npm scripts for this task is already in place.
 */

const { createWriteStream, exists } = require('fs');
const { mkdir, rm, copyFile } = require('fs/promises');
const { pipeline } = require('node:stream/promises');
const path = require('path');
const axios = require('axios');
const decompress = require('decompress');

const CACHE_DIR = path.join(__filename, '..', '..', 'cache');
const DOWNLOAD_CACHE_DIR = path.join(CACHE_DIR, 'downloads');
const FONTS_CACHE_DIR = path.join(CACHE_DIR, 'fonts');

const FONTS_NOT_AVAILABILE_IN_REPO = [
  {
    /** Name of the font, used to show what is being downloaded and also to provide a name for cache */
    fontname: 'Iosevka Web',
    /** This represents the path in the download + unzipped folder where we can find the font. Used to read the font file. */
    filePathInDownload: path.join('ttf', 'iosevka-regular.ttf'),
    /** URL to download the font file. Font is downloaded and cached under the name `fontname` */
    downloadUrl: 'https://github.com/be5invis/Iosevka/releases/download/v22.1.1/webfont-iosevka-22.1.1.zip'
  },
  {
    fontname: 'Iosevka Web',
    filePathInDownload: path.join('woff2', 'iosevka-regular.woff2'),
    downloadUrl: 'https://github.com/be5invis/Iosevka/releases/download/v22.1.1/webfont-iosevka-22.1.1.zip'
  },
  {
    fontname: 'Inter',
    filePathInDownload: path.join('Inter Hinted for Windows', 'Desktop', 'Inter-Regular.ttf'),
    downloadUrl: 'https://github.com/rsms/inter/releases/download/v3.19/Inter-3.19.zip'
  },
  {
    fontname: 'Inter',
    filePathInDownload: path.join('Inter Hinted for Windows', 'Web', 'Inter-Regular.woff2'),
    downloadUrl: 'https://github.com/rsms/inter/releases/download/v3.19/Inter-3.19.zip'
  }
];

function pathExists(path) {
  return new Promise((resolve) => {
    exists(path, resolve);
  });
}

function withZipExtension(filename) {
  return `${filename}.zip`;
}

/**
 * Creates the font cache and download cache directories
 */
async function prepareDirectories() {
  await mkdir(DOWNLOAD_CACHE_DIR, { recursive: true });
  await mkdir(FONTS_CACHE_DIR, { recursive: true });
}

/**
 * Downloads a file from provided url and stores it in the provided download path
 * @param {string} downloadUrl URL from which we need to download our file
 * @param {string} downloadPath Absolute path where we will store our downloaded file. The
 */
async function downloadFont(downloadUrl, downloadPath) {
  const fileReadStream = await axios({
    method: 'GET',
    url: downloadUrl,
    responseType: 'stream'
  });

  const writeFileStream = createWriteStream(downloadPath);

  return await pipeline([fileReadStream.data, writeFileStream]);
}

/**
 * Unzips the font zip file under the provided unzipPath
 * @param {string} zipPath Absolute path where the zip file is located
 * @param {string} unzipPath Absolute path under which the zip file must be unzipped
 */
async function unzipFont(zipPath, unzipPath) {
  return decompress(zipPath, unzipPath);
}

(async () => {
  await prepareDirectories();
  for(const { fontname, filePathInDownload, downloadUrl } of FONTS_NOT_AVAILABILE_IN_REPO) {
    console.info(`Preparing font - ${fontname}`);
    const { base: fontFilename } = path.parse(filePathInDownload);
    const fontCachePath = path.join(FONTS_CACHE_DIR, fontFilename);

    if (await pathExists(fontCachePath)) {
      console.info(`\tFont ${fontname} exists in cache`);
      continue;
    }

    const fontZipCachePath = path.join(
      DOWNLOAD_CACHE_DIR,
      withZipExtension(fontname)
    );

    if (!(await pathExists(fontZipCachePath))) {
      console.info(`\tFont ${fontname} being downloaded`);
      await downloadFont(downloadUrl, fontZipCachePath);
    }

    const fontUnzippedCachePath = path.join(DOWNLOAD_CACHE_DIR, fontname);
    await unzipFont(fontZipCachePath, fontUnzippedCachePath);

    const fontPathInDownloadsPath = path.join(fontUnzippedCachePath, filePathInDownload);
    await copyFile(fontPathInDownloadsPath, fontCachePath);
    console.info(`\tFont now ${fontname} available under path - ${fontCachePath}`);
  }
})();
