from lib import *

import scipy.sparse.linalg as linalg

if __name__ == '__main__':
    A = matrix_of('A.npz')
    f = matrix_of('f.npy')
    s = linalg.spsolve(A, f, use_umfpack=True)
    B = matrix_of('B.npz')
    g = B @ s
    C = matrix_of('C.npz')
    h = C @ g
    impact_index = ImpactEntry.index_of('index_C.csv')
    for e in impact_index:
        print('%s\t%.5f\t%s' % (e.impact_name, h[e.index], e.unit))
