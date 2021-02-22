# The openLCA matrix export format

With the openLCA matrix export you can export your product system into a set of
platform independent matrices for further processing with other tools.
Currently, the following export formats are supported:

* CSV (comma separated, UTF-8 encoded text files),
* MS Excel
* Python (the NPY (dense) and NPZ (sparse) formats of Numpy and SciPy which are
  also supported by many other languages and tools)

For these formats, the set of exported matrices and index files is always the
same, just the file extensions differ (`*.csv`, `*.xslx`, `*.npy`). With the
Python export you get also a small library (`lib.py`) and script file
(`main.py`) with some examples for using the matrices.

The exported matrices are in principle the standard matrices that are used for
matrix based LCA calculations. More details about this calculation can be found
for example in the book "The computational structure of life cycle assessment"
by Reinout Heijungs and Sangwon Sun.

## Index files

The index files contain the meta data for the rows and columns of the exported
matrices. The rows in the index files are ordered, means that the first row
(ignoring the column header) describes the first row or column  of the
respective matrix and so on. The following index files are written:

* `index_A`: This index file contains the process-product (or process-waste)
  pairs that are related to the rows and columns of the technosphere matrix `A`.
  Note that it can contain the same processes with different product (or waste)
  flows when there are multi-functional processes in the system.
* `index_B`: This index file contains the (elementary) flows that are related to
  the rows of the intervention matrix `B`. In case of a regionalized system,
  this file contains the flow-location pairs so that a flow can occur multiple
  times with different locations.
* `index_C`: This index file is only present when you exported a product system
  together with an impact assessment method. It contains then the impact
  categories that are related to the rows of the matrix `C`.

## Matrix files

The following matrix files are written in the export:

* `A`: This is the technosphere matrix that contains inputs and outputs of
  product and waste flows of the processes in the system. It is a square matrix
  where inputs are denoted as negative and outputs as positive values.
* `B`: This is the elementary flow * process-product matrix that contains the
  inputs and outputs of the elementary flows of the processes in the system.
  Again, inputs are denoted as negative and outputs as positive values.
* `C`: This matrix is only present when you export the matrices with an impact
  assessment method. It is an impact category * elementary flow matrix that
  contains the characterization factors of the flows related to the respective
  impact categories. The impact direction is denoted by the sign of the
  characterization factor.
* `f` is the final demand vector that contains the quantitative reference of the
  system.

With these matrices, you can calculate the inventory (LCI) result `g` as:

    g = B * (A^-1 * f)

The impact assessment result `h` can be calculated via:

    h = C * g

## Uncertainty distributions

When you export the matrices with their uncertainty distributions, the export
will generate additional matrices for the matrices `A`, `B`, and `C` that
contain the distribution parameters of the corresponding matrix values. The
matrices `A_utype`, `B_utype`, and `C_utype` contain the distribution types of
the corresponding cells in the matrices `A`, `B`, and `C`. The distribution
types in a `*_utype` matrix are encoded with the following values:

- `0`: no uncertainty distribution
- `1`: log-normal distribution
- `2`: normal distribution
- `3`: triangular distribution
- `4`: uniform distribution

The matrices `A_u0`, `A_u1`, `B_u0`, etc. contain the respective parameter
values of the distributions for the corresponding matrix cells. The following
distribution parameters are contained in these matrices:

- `*_u0`:
  - log-normal: geometric mean
  - normal: arithmetic mean
  - uniform: minimum
  - triangular: minimum
- `*_u1`:
  - log-normal: geometric standard deviation
  - normal: arithmetic standard deviation
  - uniform: maximum
  - triangular: mode
- `*_u2`:
  - triangular: maximum
