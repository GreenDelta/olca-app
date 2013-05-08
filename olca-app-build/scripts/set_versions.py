import os

version = "1.2.8"

def changePluginManifest(filePath):
    f = open(filePath, 'rb')
    lines = f.readlines()
    f = open(filePath, 'wb')
    for line in lines:
        if line.startswith("Bundle-Version:"):
            f.write("Bundle-Version: " + version + "\n")
        else:
            f.write(line)

for dir in os.listdir("../.."):
    if dir.startswith("org.openlca."):
        path = "../../" + dir + "/META-INF/MANIFEST.MF"
        if os.path.isfile(path):
            changePluginManifest(path)
        

    