"""
This is an example module how you can use the openLCA matrix export from Python.
It is part of the openLCA source code which is licensed under the Mozilla Public
License 2.0 (MPL 2.0; see https://github.com/GreenDelta/olca-app).
"""
from __future__ import annotations

import csv
import os
from typing import Iterator, List

import numpy
import numpy.linalg
import scipy.sparse


class TechEntry:
    """
    A TechEntry contains the meta data of a row or column of the technosphere
    matrix A.
    """

    def __init__(self):
        self.index = -1
        self.process_id = ''
        self.process_name = ''
        self.process_category = ''
        self.process_location = ''
        self.flow_id = ''
        self.flow_name = ''
        self.flow_category = ''
        self.flow_unit = ''
        self.flow_type = ''

    @staticmethod
    def _from_csv(row: List[str]) -> TechEntry:
        e = TechEntry()
        e.index = int(row[0])
        e.process_id = row[1]
        e.process_name = row[2]
        e.process_category = row[3]
        e.process_location = row[4]
        e.flow_id = row[5]
        e.flow_name = row[6]
        e.flow_category = row[7]
        e.flow_unit = row[8]
        e.flow_type = row[9]
        return e

    @staticmethod
    def index_of(file_path: str) -> List[TechEntry]:
        index = []
        for row in _csv_rows_of(file_path):
            index.append(TechEntry._from_csv(row))
        return index


class FlowEntry:
    """
    A FlowEntry contains the meta data of a row in the intervention matrix B.
    """

    def __init__(self):
        self.index = -1
        self.flow_id = ''
        self.flow_name = ''
        self.flow_category = ''
        self.flow_unit = ''
        self.flow_type = ''
        self.location_id = ''
        self.location_name = ''
        self.location_code = ''

    @staticmethod
    def _from_csv(row: List[str]) -> FlowEntry:
        e = FlowEntry()
        e.index = int(row[0])
        e.flow_id = row[1]
        e.flow_name = row[2]
        e.flow_category = row[3]
        e.flow_unit = row[4]
        e.flow_type = row[5]
        e.location_id = row[6]
        e.location_name = row[7]
        e.location_code = row[8]
        return e

    @staticmethod
    def index_of(file_path: str) -> List[FlowEntry]:
        index = []
        for row in _csv_rows_of(file_path):
            index.append(FlowEntry._from_csv(row))
        return index


class ImpactEntry:
    """
    An ImpactEntry contains the meta data of a row in the characterization
    matrix C.
    """

    def __init__(self):
        self.index = -1
        self.impact_id = ''
        self.impact_name = ''
        self.impact_unit = ''

    @staticmethod
    def _from_csv(row: List[str]) -> ImpactEntry:
        e = ImpactEntry()
        e.index = int(row[0])
        e.impact_id = row[1]
        e.impact_name = row[2]
        e.impact_unit = row[3]
        return e

    @staticmethod
    def index_of(file_path: str) -> List[ImpactEntry]:
        index = []
        for row in _csv_rows_of(file_path):
            index.append(ImpactEntry._from_csv(row))
        return index


def matrix_of(file_path: str):
    if file_path.endswith('.npz'):
        return scipy.sparse.load_npz(file_path)
    return numpy.load(file_path)


def _csv_rows_of(f: str) -> Iterator[List[str]]:
    with open(f, 'r', encoding='utf-8') as stream:
        reader = csv.reader(stream)
        next(reader)  # skip header
        for row in reader:
            yield row


class ExportFolder:

    def __init__(self, folder: str):
        self.folder = folder

    def tech_index(self) -> List[TechEntry]:
        path = os.path.join(self.folder, 'index_A.csv')
        if not os.path.exists(path):
            return []
        return TechEntry.index_of(path)

    def flow_index(self) -> List[FlowEntry]:
        path = os.path.join(self.folder, 'index_B.csv')
        if not os.path.exists(path):
            return []
        return FlowEntry.index_of(path)

    def impact_index(self) -> List[ImpactEntry]:
        path = os.path.join(self.folder, 'index_C.csv')
        if not os.path.exists(path):
            return []
        return ImpactEntry.index_of(path)

    def has_impacts(self):
        path = os.path.join(self.folder, 'index_C.csv')
        return os.path.exists(path)

    def load(self, name: str):
        path = os.path.join(self.folder, name)
        if os.path.exists(path):
            return matrix_of(path)
        p = path + '.npy'
        if os.path.exists(p):
            return matrix_of(p)
        p = path + '.npz'
        if os.path.exists(p):
            return matrix_of(p)
        return None


class Matrix:
    A = 'A'
    B = 'B'
    C = 'C'
    f = 'f'


def _as_dense(matrix):
    if scipy.sparse.issparse(matrix):
        return matrix.todense()
    return matrix


def solve(matrix, f):
    """Note that we currently convert sparse matrices to a dense format."""
    return numpy.linalg.solve(_as_dense(matrix), f)


def invert(matrix):
    return numpy.linalg.inv(_as_dense(matrix))


class UpstreamNode:

    def __init__(self):
        self.index = -1
        self.result = 0.0
        self.scaling = 0.0
        self.childs: List[UpstreamNode] = []


class UpstreamTree:

    def __init__(self):
        self.root = UpstreamNode()
        self.intensities = None
        self.tech_matrix = None

    @staticmethod
    def of_impact(folder: ExportFolder, i: int) -> UpstreamTree:
        tech_matrix = _as_dense(folder.load(Matrix.A))
        flow_matrix = folder.load(Matrix.B)
        factors = folder.load(Matrix.C)
        inverse = invert(tech_matrix)
        intensities = flow_matrix @ inverse
        impacts = (factors @ intensities)[i, :]
        demand = folder.load(Matrix.f)[0]
        root = UpstreamNode()
        root.index = 0
        root.result = demand * impacts[0, 0]
        root.scaling = demand / tech_matrix[0, 0]
        tree = UpstreamTree()
        tree.root = root
        tree.intensities = impacts
        tree.tech_matrix = tech_matrix
        return tree

    def expand(self, parent: UpstreamNode) -> List[UpstreamNode]:
        if parent is None or parent.index < 0:
            return []
        parent.childs = []
        tech_column = self.tech_matrix[:, parent.index]
        for i in range(0, len(tech_column)):
            if i == parent.index:
                continue
            aij = tech_column[i]
            intensity = self.intensities[0, i]
            if aij == 0 or intensity == 0:
                continue
            aij = aij * parent.scaling
            ref_val = self.tech_matrix[i, i]
            child = UpstreamNode()
            child.index = i
            child.scaling = -aij / ref_val
            child.result = intensity * ref_val * child.scaling
            parent.childs.append(child)

        parent.childs.sort(key=lambda n: n.result, reverse=True)
        return parent.childs
