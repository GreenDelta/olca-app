import os

from pathlib import Path


# the root of the build project olca-app/olca-app-build
PROJECT_DIR = Path(os.path.abspath(__file__)).parent.parent

# the version of the native library package
BLAS_JNI_VERSION = "0.0.1"

# the bundle ID of the JRE
JRE_ID = "org.openlca.jre"
