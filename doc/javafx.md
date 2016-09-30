# JavaFX in openLCA
Since openLCA 1.6 we use JavaFX components in openLCA. Enabling JavaFX and the
JavaFX bridge in openLCA is a bit tricky because we are running on the Eclipse 4
platform using the compatibility layer for 3.x applications.

* fx hook-plugins + startup flags
* JRE configuration for build (https://bugs.eclipse.org/bugs/attachment.cgi?id=247744)
