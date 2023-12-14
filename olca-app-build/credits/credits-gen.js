// This script builds the `about.html` page that contains the license
// information and third party credits. When building the `about.html` page the
// respective third party licenses are fetched from the respective websites and
// cached in the `credits/cache` folder. This cache folder and the about page
// are not put under version control.

// in order to collect our Maven dependencies, you can run
// mvn project-info-reports:dependencies

const fs = require('fs/promises');
const path = require('path');
const http = require('http');
const https = require('https');
const AdmZip = require("adm-zip");

async function main() {
  console.log("build credits file: about.html");

  // make sure that the `cache` folder exists
  const cacheDir = path.join(__dirname, 'cache');
  await fs.mkdir(cacheDir).catch(() => { });

  // generate the credit blocks
  const creditsPath = path.join(__dirname, 'credits.json');
  const { credits, pipCredits } = JSON.parse(await fs.readFile(creditsPath, 'utf8'));
  const allCredits = [...credits, ...pipCredits];
  allCredits.sort((c1, c2) => {
    const p1 = c1.project.trim().toLowerCase();
    const p2 = c2.project.trim().toLowerCase();
    return p1.localeCompare(p2);
  });
  let creditBlocks = '';
  for (const credit of allCredits) {
    const licenseText = await getLicenseText(credit);
    creditBlocks += creditBlockOf(credit, licenseText);
  }

  // generate the file
  const templatePath = path.join(__dirname, 'about_template.html');
  const template = await fs.readFile(templatePath, 'utf8');
  const aboutText = template.replace('#{credits}#', creditBlocks);
  const aboutPath = path.join(__dirname, 'about.html');
  await fs.writeFile(aboutPath, aboutText);
}

async function getLicenseText(credit) {
  const { project, licenseUrl, pipUrl } = credit;
  const cachedFile = path.join(__dirname, 'cache', `${project}_license.txt`);
  try {
    return await fs.readFile(cachedFile);
  } catch {
    const licenseText = licenseUrl != null
      ? await fetchLicenseFrom(licenseUrl)
      : await fetchPipLicenseFrom(pipUrl);
    await fs.writeFile(cachedFile, licenseText);
    return licenseText;
  }
}

function fetchLicenseFrom(url) {
  console.log(`  fetch license: ${url}`);
  function makeRequest(url, resolve, reject) {
    const client = url.startsWith('http://') ? http : https;
    client.get(url, (resp) => {
      if (resp.statusCode === 301 || resp.statusCode === 302) {
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

function fetchPipLicenseFrom(url) {
  console.log(`  fetch pip package: ${url}`);

  function makeRequest(url, resolve, reject) {
    const client = url.startsWith('http://') ? http : https;
    client.get(url, (resp) => {
      if (resp.statusCode === 301 || resp.statusCode === 302) {
        return makeRequest(resp.headers.location, resolve, reject)
      } else {
        let data = []
        resp.on('data', (chunk) => data.push(chunk))
        resp.on('end', () => resolve(getLicenseFromPackage(data)))
      }
    }).on("error", (err) => reject(err));
  }

  return new Promise((resolve, reject) => makeRequest(url, resolve, reject));
}

async function getLicenseFromPackage(data) {
  const buf = Buffer.concat(data);
  const zip = new AdmZip(buf);
  for (entry of zip.getEntries())
    if (entry.name.endsWith("LICENSE.txt")) {
      return entry.getData().toString("utf8");
    }
}

function creditBlockOf(credit, licenseText) {
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
          <pre style='word-wrap: break-word; white-space: pre-wrap;'>${licenseText}</pre>
        </div>
      </div>
    </div>
  `
}

main();
