const fs = require('fs')
const path = require('path')

const dir = path.join(__dirname, './msg')


// const data = fs.readFileSync(dir + '/messages.properties', {encoding: 'utf-8'})
// console.log(data.split("\n")[0].trim())

const readMessageMap = (file) => {
    const text = fs.readFileSync(file, { encoding: 'utf-8' })
    return text.split("\n")
        .map(line => {
            const t = line.trim()
            const cut = t.indexOf("=")
            if (t.length === 0 || cut < 0) {
                return null
            }
            const key = t.substring(0, cut)
            const val = t.substring(cut + 1, t.length)
            return [key.trim(), val.trim()]
        })
        .filter(obj => obj !== null)
        .reduce((m, [key, val]) => {
            m[key] = val
            return m
        }, {})
}

const en = readMessageMap(dir + '/messages.properties')
const messages = { en }

for (const file of fs.readdirSync(dir)) {
    if (!file.startsWith('messages_')) {
        continue
    }
    const code = file.substring(9).split('.')[0]
    const msgs = readMessageMap(dir + '/' + file)
    Object.keys(en).forEach(key => {
        if (!msgs[key]) {
            msgs[key] = en[key]
        }
    })
    messages[code] = msgs
}

fs.writeFileSync(
    path.join(__dirname, '../../dist/messages.json'),
    JSON.stringify(messages, null, 2), { encoding: 'utf-8' })
