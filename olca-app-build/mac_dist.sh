#!/bin/bash

APP_SUFFIX=$(python3 -m package.dist -v)
APP_ID="org.openlca.app"
JRE_ID="org.openlca.jre"
APP_DMG="build/tmp/macosx.cocoa.x86_64/openLCA_dmg/openLCA.app"
APP_PKG="build/tmp/macosx.cocoa.x86_64/openLCA_pkg/openLCA.app"
APP_UNSIGNED="build/openLCA.app"
MKL_LIBS=("${APP_UNSIGNED}"/Contents/Eclipse/olca-mkl*)
LIB=$(if [ -d "$MKL_LIBS" ]; then echo "mkl_"; else echo ""; fi)
DMG="build/dist/openLCA_${LIB}macOS_x64_${APP_SUFFIX}.dmg"
PKG="build/dist/openLCA_${LIB}macOS_x64_${APP_SUFFIX}.pkg"
ENTITLEMENTS_DMG="resources/dmg.entitlements"
ENTITLEMENTS_PKG="resources/pkg.entitlements"
KEY_NOTARYTOOL="notarytool"
KEY_ALTOOL="altool"

# Image disk parameters
BACKGROUND_DMG="resources/background_dmg.png"
VOLUME_ICON_FILE="$APP_UNSIGNED/Contents/Resources/logo.icns"
VOLUME_NAME="openLCA Installer"


clean() {
  printf "Removing previous files... "
  rm -f jars.list libraries.list
  rm -rf tmp
  rm -f "$PRODUCT"
  rm -rf "${APP%/*.*}"
  printf " Done.\n"
}

cp_app() {
  printf "\nCopying the unsigned app... "
  mkdir -p "${APP%/*.*}"
  cp -r "$APP_UNSIGNED" "$APP"
  printf " Done.\n"
}

sign_lib_pkg() {
  printf "\nSigning the frameworks and libraries with codesign...\n"
  find "${APP}/" -depth \
   -not -path "*jre/Contents*" \
   -name "*.dylib" \
   -or -name "*.bundle" \
   -or -name "*.so" \
   -or -name "*.jnilib" |
    while read -r file;
    do
      codesign -f -v --timestamp --options runtime -i "$APP_ID" \
       -s "$APP_CERT" "$file";
    done

    find "${APP}/Contents/Eclipse/jre" -depth \
     -name "*.framework" \
     -or -name "*.dylib" \
     -or -name "*.bundle" \
     -or -name "*.so" \
     -or -name "*.jnilib" \
     -or -name "*jspawnhelper" |
      while read -r file;
      do
        codesign -f -v --options runtime --entitlements "$ENTITLEMENTS" \
         -i "$JRE_ID" -s "$APP_CERT" "$file";
      done

  printf "\nSigning the JRE binaries with codesign...\n"
  JRE="${APP}/Contents/Eclipse/jre/Contents/Home/bin/"
  find "$JRE" -type f |
    while read -r file;
      do
        codesign -f -v --timestamp --options runtime \
         --entitlements "$ENTITLEMENTS" \
         -i "net.java.openjdk.$(basename "$file")" -s "$APP_CERT" "$file";
      done
}

sign_lib_dmg() {
   printf "\nSigning the frameworks and libraries with codesign...\n"
   find "${APP}/" -depth \
    -name "*.framework" \
    -or -name "*.dylib" \
    -or -name "*.bundle" \
    -or -name "*.so" \
    -or -name "*.jnilib" |
     while read -r file;
     do
       codesign -f -v --timestamp --options runtime  -i "$APP_ID" \
        -s "$APP_CERT" "$file";
     done
}

sign_jar() {
  printf "\nSigning all the libraries contained into .jar files.\n"
  # Find all the .jar files in the application
  find "$APP" -depth -name "*.jar" > jars.list

  # Loop over all the .jar files
  while IFS="" read -r p || [ -n "$p" ]
    do
      printf "Checking %s...\n" "$p"

      # List all the libraries that need to be sign.
      jar tf "$p" | grep -E '.so$|.framework$|.dylib$|.bundle$|.jnilib$' \
       > libraries.list

      # Loop over the libraries of that JAR.
      while IFS="" read -r file || [ -n "$file" ]
        do
          printf "  Signing %s...\n  " "$file"
          # Extract the library
          mkdir tmp
          cd tmp || exit 0
          jar xf "../${p}" "$file"
          cd ..
          # Sign the library
          codesign -f -v --timestamp --options runtime  -i "$APP_ID" \
           -s "$APP_CERT" "tmp/${file}"
          # Update the JAR with the signed library
          jar uf "$p" "tmp/${file}"
          rm -r tmp
        done < libraries.list

      rm -f libraries.list
    done < jars.list

    rm -f jars.list
}

build_pkg() {
  printf "\nCreating the package installer file...\n"
  productbuild --sign "$STORE_INSTALLER_CERT" --component "$APP" /Applications \
   "$PKG"
}

build_dmg() {
  NOTARIZATION_DMG="${APP%/*.*}/notarization.dmg"

  rm -f "$NOTARIZATION_DMG"

  printf "\nCreating the disk image installer file to be notarized...\n"
  hdiutil create -srcfolder "${APP%/*.*}/" \
   -volname "$(basename "NOTARIZATION_DMG")" -fs "HFS+" "$NOTARIZATION_DMG"

  printf "\nNotarization of the DMG...\n"
  xcrun notarytool submit "$NOTARIZATION_DMG" \
   --keychain-profile "$KEY_NOTARYTOOL" --wait  || exit 1

  printf "\nStapling the app...\n"
  xcrun stapler staple "$APP" || exit 1

  printf "\nCreating the disk image installer file to be distributed...\n"
  create-dmg \
   --volname "$VOLUME_NAME" \
   --background "$BACKGROUND_DMG" \
   --volicon "$VOLUME_ICON_FILE" \
   --window-pos 200 120 --window-size 800 400 --icon-size 150 \
   --icon "$(basename "$APP")" 200 160 \
   --app-drop-link 600 155 \
   "$DMG" "$APP"

  printf "\n If notarization failed, see the details by running: "
  printf "\n xcrun notarytool log  --keychain-profile <name> <REQUEST_ID>"
}

notarize() {
  if [ "$1" = "pkg" ]; then
    APP="$APP_PKG"
    PRODUCT="$PKG"
    APP_CERT="$STORE_APP_CERT"
    ENTITLEMENTS="$ENTITLEMENTS_PKG"
  elif [ "$1" = "dmg" ]; then
    APP="$APP_DMG"
    PRODUCT="$DMG"
    APP_CERT="$DEV_APP_CERT"
    ENTITLEMENTS="$ENTITLEMENTS_DMG"
  fi

  clean
  cp_app

  printf "\nConverting the XML files to the right format...\n"
  plutil -convert xml1 "$ENTITLEMENTS"
  plutil -convert xml1 "${APP}/Contents/Info.plist"

  printf "\nRemoving eventual quarantine attribute...\n"
  xattr -rc "$APP"

  if [ "$1" = "pkg" ]; then
      sign_lib_pkg
    elif [ "$1" = "dmg" ]; then
      sign_lib_dmg
  fi
  sign_jar

  printf "\nSigning the openLCA executable at runtime...\n"
  codesign -f -v --deep --entitlements "$ENTITLEMENTS" --timestamp \
   --options runtime -i "$APP_ID" -s "$APP_CERT" "${APP}/Contents/MacOS/openLCA"

  printf "\nSigning the app bundle with the certificate...\n"
  codesign -f -v --entitlements "$ENTITLEMENTS" --timestamp --options runtime \
   -i "$APP_ID" -s "$APP_CERT" "$APP"
  printf "\nChecking signature of the bundle...\n"
  codesign -dvv "$APP"

  if [ "$1" = "pkg" ]; then
    build_pkg
  elif [ "$1" = "dmg" ]; then
    build_dmg
  fi

  printf "\nEnd of packaging %s. Please test before distributing.\n" "$PRODUCT"
}

upload_pkg() {
  printf "\nValidating and uploading the app to the App Store...\n"
  xcrun altool --validate-app -f "$PKG" -t osx -u "$USER" \
    -p @keychain:"$KEY_ALTOOL"

  xcrun altool --upload-app -f "$PKG" -t osx -u "$USER" \
    -p @keychain:"$KEY_ALTOOL"
}

usage() {
	cat <<EOHELP

Create Mac distributions for openLCA.

Usage:  $0 [args] [<pkg>|<dmg>|<upload>]

<pkg> sign the code to create a .pkg installer, validate and upload the app to
 the App Store.
  --store-id-app <id>
    Apple ID to sign the application for the Apple Store ("3rd Party Mac
    Developer Application: GreenDelta GmbH (<code>)")
  --store-id-installer <id>
      Apple ID to sign the installer for the Apple Store ("3rd Party Mac
      Developer Installer: GreenDelta GmbH (<code>)")
  --user <Apple Developer ID>
      E-mail address of the Apple developer account.

<dmg> sign the code to create a .dmg disk image, notarize and staple the app.
  --dev-id-app <id>
      Apple ID to sign the application for the independent distribution
      ("Developer ID Application: GreenDelta GmbH (<code>)")

<upload> validate and upload the .pkg app to the App Store.

EOHELP
	exit 0
}

# Argument parsing
while [[ "${1:0:1}" = "-" ]]; do
	case $1 in
		--store-id-app)
			STORE_APP_CERT="$2"
			shift; shift;;
		--store-id-installer)
			STORE_INSTALLER_CERT="$2"
			shift; shift;;
		--dev-id-app)
		  DEV_APP_CERT="$2"
      shift; shift;;
    --user)
      USER="$2"
      shift; shift;;
    --help | -h)
      usage;;
    -*)
      echo "Unknown argument: $1. Run $0 --help to see usage."
      exit 1;;
  esac
done


if [ "$1" = "pkg" ]; then
  if [ -z "$STORE_APP_CERT" ] || [ -z "$STORE_INSTALLER_CERT" ] \
   || [ -z "$USER" ]; then
    echo "Missing argument. Run $0 --help to see usage." && exit 1
  fi
  notarize "pkg"
elif [ "$1" = "dmg" ]; then
  if [ -z "$DEV_APP_CERT" ]; then
    echo "Missing argument. Run $0 --help to see usage." && exit 1
  fi
  notarize "dmg"
elif [ -z "$1" ]; then
  usage
fi

if [ "$1" = "pkg" ] || [ "$1" = "upload" ]; then
  while true; do
      printf "\n"
      read -rp "Do you wish to upload openLCA to the App Store? (Y/n)" answer
      case $answer in
          Y ) upload_pkg; break;;
          n ) exit;;
          * ) echo "Please answer Y or n.";;
      esac
  done
fi
