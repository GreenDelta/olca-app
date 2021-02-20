"""
This script gives some examples for using the openLCA matrix export in Python.
You need to have NumPy and SciPy installed, e.g. via pip

  pip install -U numpy scipy

Note that all calculations in these examples are currently done with dense
matrices. Sparse matrices are converted to a dense format in these calculations.
If you want to use faster calculations for sparse matrices checkout the direct
and iterative solvers from the SciPy package:

  https://docs.scipy.org/doc/scipy/reference/sparse.linalg.html
"""

import random
from lib import *


def lcia_example():
    """Calculates and prints the LCIA result."""
    folder = ExportFolder('.')
    if not folder.has_impacts():
        print('error: no impacts in your export')
        return

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


def upstream_tree_example():
    """An example how you can build and use upstream trees."""
    folder = ExportFolder('.')
    if not folder.has_impacts():
        print('error: no impacts in your export')
        return

    impacts = folder.impact_index()
    impact_idx = random.randint(0, len(impacts) - 1)
    impact = impacts[impact_idx]
    print('Calculate upstream tree for:', impact.impact_name)

    # when there are cycles in the system our upstream tree has an infinite
    # depth. in this case we need to set some cut-off values
    tree = UpstreamTree.of_impact(folder, impact_idx)
    tech_index = folder.tech_index()
    max_depth = 5
    cutoff = 0.1

    def expand(node: UpstreamNode, depth=0):
        if depth > max_depth:
            return
        product = tech_index[node.index]
        print('+%s %s: %.5f %s' % (
            '-' * depth,
            product.process_name,
            node.result,
            impact.impact_unit))
        if node.result == 0:
            return
        for child in tree.expand(node):
            c = abs(child.result / node.result)
            if c < cutoff:
                continue
            expand(child, depth + 1)

    expand(tree.root)


if __name__ == '__main__':
    # lcia_example()
    upstream_tree_example()
