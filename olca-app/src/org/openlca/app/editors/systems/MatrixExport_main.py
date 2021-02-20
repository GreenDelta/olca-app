"""
This script gives some examples for using the openLCA matrix export from Python.
You need to have NumPy and SciPy installed, e.g. via pip

  pip install -U numpy scipy

Note that all caclulations in these examples are currently done with dense
matrices. Sparse matrices are converted to a dense format in these calculations.
If you want to use faster calculations for sparce matrices checkout the direct
and iterative solvers from the SciPy package:

https://docs.scipy.org/doc/scipy/reference/sparse.linalg.html
"""

from lib import *

def lcia_example():
    """Calculates and prints the LCIA result."""
    folder = ExportFolder('.')

    # load the matrices
    A = folder.load(Matrix.A)
    B = folder.load(Matrix.B)
    C = folder.load(Matrix.C)
    f = folder.load(Matrix.f)

    # calculate the LCIA result
    scaling = solve(A, f)
    g = B @ scaling
    h = C @ g

    for i in folder.impact_index():
        print('%s , %.5f , %s' % (i.impact_name, h[i.index], i.impact_unit))


if __name__ == '__main__':
    lcia_example()
