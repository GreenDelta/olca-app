const fs = require('fs/promises');
const path = require('path');
const http = require('http');
const https = require('https');
const creditsJsonPath = path.join(__dirname, 'credits.json');
const cacheDirPath = path.join(__dirname, 'cache');
const aboutTemplatePath = path.join(__dirname, 'about_template.html');
const aboutHtmlPath = path.join(__dirname, 'about.html');

/** Used to generate a normalised name for license cache file */
function cacheFilename(projectName) {
  return `${projectName.split(' ').join('_')}_license.txt`;
}

/** Fetches a license from url and returns the text */
function fetchLicense(url) {
  function makeRequest(url, resolve, reject) {
    const requestClient = url.startsWith('http://') ? http : https;
    requestClient.get(url, (resp) => {
      if(resp.statusCode === 301 || resp.statusCode === 302) {
        return makeRequest(resp.headers.location, resolve, reject)
      } else {
        let data = '';
        resp.on('data', (chunk) => data += chunk);
        resp.on('end', () => resolve(data));
      }
    }).on("error", (err) => reject(err));
  }

  return new Promise((resolve, reject) => makeRequest(url, resolve, reject));
}

/** Generates a file under cache directory containing the license text */
async function cacheLicense(licenseFilename, licenseText) {
  const licenseCachePath = path.join(cacheDirPath, licenseFilename);
  return await fs.writeFile(licenseCachePath, licenseText);
}

/** Fetches license from cache if available otherwise retrieves it from internet and caches it */
async function getLicenseText(credit) {
  const { project, licenseUrl } = credit;
  const cacheFile = cacheFilename(project);
  const licenseCachePath = path.join(cacheDirPath, cacheFile);
  try {
    return await fs.readFile(licenseCachePath);
  } catch(error) {
    const licenseTxt = await fetchLicense(licenseUrl);
    await cacheLicense(cacheFile, licenseTxt);
    return licenseTxt;
  }
}

/** Generates html block that also contains the license text */
function generateHtmlForCredit(credit, licenseTxt) {
  return `
    <div style='width: 100%; margin-bottom: 10px;'>
      <div class='block'>
        <div class='mr-pd'>
          <span>
            ${credit.project}
            (licensed under <a target="_blank" href="${credit.licenseUrl}">${credit.license}</a>)
          </span>
        </div>
        <div style='display: flex; flex-direction: row;'>
          <div class='mr-pd pointer'>
            <span data-action-id='${credit.project}'>
              show licence
            </span>
          </div>
          <div class='mr-pd'>
            <a target='_blank' href='${credit.projectUrl}'>homepage</a>
          </div>
        </div>
      </div>
      <div data-description='licence-content' data-id='${credit.project}' class='licence-details hide'>
        <div>
          <pre style='word-wrap: break-word; white-space: pre-wrap;'>${licenseTxt}</pre>
        </div>
      </div>
    </div>
  `
}

/** Injects the html code generated from `generateHtmlForCredit` in template file and writes it */
async function generateAboutHtmlWithLicenseText(licenseHtml) {
  const templateHtml = await fs.readFile(aboutTemplatePath, 'utf8');
  const aboutHtml = templateHtml.replace('#{credits}#', licenseHtml);
  await fs.writeFile(aboutHtmlPath, aboutHtml);
}

// Main function that reads credits from json and generates an html file for it
(async () => {
  const { credits } = JSON.parse(await fs.readFile(creditsJsonPath, 'utf8'));
  await fs.mkdir(cacheDirPath).catch(() => { /* Ignore */ })
  let licenseHtml = '';
  for(const credit of credits) {
    const licenseTxt = await getLicenseText(credit);
    licenseHtml += generateHtmlForCredit(credit, licenseTxt);
  }
  await generateAboutHtmlWithLicenseText(licenseHtml);
})();
