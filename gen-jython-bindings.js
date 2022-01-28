// generates import declarations for the Jython
// interpreter

const fs = require('fs')

// checks if the given thing is a Java file
let isFile = file =>
  fs.statSync(file).isFile()
    && file.endsWith('.java')

// checks if the given thing is a folder
let isDir = file =>
  fs.statSync(file).isDirectory()

// scans the given source code folder
let scan = (folder, package, bindings) => {
  if (!isDir) {
    console.log(`skipped ${folder}; does not exist`)
    return
  }

  let files = fs.readdirSync(folder)

  // first scan sub folders
  for (let file of files) {
    let path = `${folder}/${file}`
    if (!isDir(path))
      continue
    let subpack = package !== ''
      ? `${package}.${file}`
      : file
    scan(path, subpack, bindings)
  }

  // scan java class files
  for (let file of files) {
    let path = `${folder}/${file}`
    if (!isFile(path))
      continue

    // read all lines
    let lines = fs.readFileSync(path, {
        encoding: 'utf-8',
    }).split('\n')
      .map(line => line.trim())

    // search for public class declarations
    for (let line of lines) {
      let match = line.match(
        /public( final)?( abstract)? (?:class|enum|record) ([a-zA-Z0-9]*)(.*)?/)
      if (!match || match.length < 4)
        continue
      let clazz = match[3]
      let fullName = `${package}.${clazz}`
      let binding = bindings[clazz]
      if (binding) {
        console.log(
          `skip ${fullName};`,
          ` existing binding: ${binding}`)
        break
      }
      bindings[clazz] = fullName
      break
    }
  }
}

let generate = (folders, target) => {
  let bindings = {}
  for (let folder of folders) {
    scan(folder, '', bindings)
  }
  let text = Object.keys(bindings)
    .sort()
    .map(clazz => `import ${bindings[clazz]} as ${clazz}\n`)
    .reduce((t, s) => t.concat(s))
  let out = `olca-app/src/org/openlca/app/devtools/python/${target}`
  let header = '# auto-generated bindings; do not edit them\n'
  fs.writeFileSync(out, header + text)
}

let main = () => {
  let modPath = '../olca-modules'
  // duplicate class names are skipped, so
  // the order of the folders is important
  let modFolders = [
    `${modPath}/olca-core/src/main/java`,
    `${modPath}/olca-formula/src/main/java`,
    `${modPath}/olca-io/src/main/java`,
    `${modPath}/olca-proto-io/src/main/java`,
    `${modPath}/olca-ipc/src/main/java`,
    // `${modPath}/olca-ilcd/src/main/java`,
    // `${modPath}/olca-simapro-csv/src/main/java`,
    // `${modPath}/olca-ecospold-1/src/main/java`,
    // `${modPath}/olca-ecospold-2/src/main/java`,
	  // 'olca-app/src',
  ]
  generate(modFolders, 'mod_bindings.py')
}

main()
