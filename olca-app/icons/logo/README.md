# Create icons for the distribution

## macOS

In order to generate the `.icns` of the macOS distribution, create a 1024x1024 PNG image and use the script from `olca-app-build/create_icns.sh`:
```bash
create_icns.sh logo.png
```

## Windows

Multiple steps are necessary to combine the different icons into a `.ico` file:

1. Create 16x16, 32x32, 48x48 and 256x256 PNG icons with an alpha layer (transparency) (in 32 bit).
2. [conversion into 8 bit BMP] In GIMP, convert the 16x16, 32x32 and 48x48 icons into 8 bit BMP by removing the alpha layer (transparency) and using the _indexed_ mode.
3. [conversion into 32 bit BMP] In GIMP again, convert the 16x16, 32x32, 48x48 and 256x256 PNG icons into BMP while keeping the alpha layer and use the _RGB_ mode.
4. [conversion to Windows `.ico`] Use a converter or the following macOS command to pack the 7 icons into one ICO file.

```bash
convert 16_8bit.bmp 16_32bit.bmp 32_8bit.bmp 32_32bit.bmp 48_8bit.bmp 48_32bit.bmp 256_32bit.bmp logo.ico
```
