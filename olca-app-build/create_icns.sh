#!/bin/bash
# create a ICNS file from a 1024x1024 icon with the required image sizes.

OUTPUT_PATH=logo.iconset
LOGO=$1

mkdir -p "$OUTPUT_PATH"
# the convert command comes from imagemagick
for size in 16 32 128 256 512; do
  double="$((size * 2))"
  convert "$LOGO" -resize x$size "$OUTPUT_PATH/icon_${size}x${size}.png"
  convert "$LOGO" -resize x$double "$OUTPUT_PATH/icon_${size}x${size}@2x.png"
done

iconutil -c icns $OUTPUT_PATH
rm -r "$OUTPUT_PATH"
