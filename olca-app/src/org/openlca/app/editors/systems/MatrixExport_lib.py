"""
This is an example module how you can use the openLCA matrix export from Python.
It is part of the openLCA source code which is licensed under the Mozilla Public
License 2.0 (MPL 2.0; see https://github.com/GreenDelta/olca-app).
"""
from __future__ import annotations

import csv
import os
from typing import Iterator, List, Tuple

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
        self.process_type = ''
        self.process_location = ''
        self.process_category = ''
        self.flow_id = ''
        self.flow_name = ''
        self.flow_type = ''
        self.flow_location = ''
        self.flow_category = ''
        self.flow_unit = ''

    @staticmethod
    def _from_csv(index: int, row: List[str]) -> TechEntry:
        e = TechEntry()
        e.index = index
        e.process_id = row[0]
        e.process_name = row[1]
        e.process_type = row[2]
        e.process_location = row[3]
        e.process_category = row[4]
        e.flow_id = row[5]
        e.flow_name = row[6]
        e.flow_type = row[7]
        e.flow_location = row[8]
        e.flow_category = row[9]
        e.flow_unit = row[10]
        return e

    @staticmethod
    def index_of(file_path: str) -> List[TechEntry]:
        index = []
        for (i, row) in _csv_rows_of(file_path):
            index.append(TechEntry._from_csv(i, row))
        return index


class FlowEntry:
    """
    A FlowEntry contains the meta data of a row in the intervention matrix B.
    """

    def __init__(self):
        self.index = -1
        self.flow_id = ''
        self.flow_name = ''
        self.flow_type = ''
        self.flow_category = ''
        self.flow_unit = ''
        self.location = ''

    @staticmethod
    def _from_csv(index: int, row: List[str]) -> FlowEntry:
        e = FlowEntry()
        e.index = index
        e.flow_id = row[0]
        e.flow_name = row[1]
        e.flow_type = row[2]
        e.flow_category = row[3]
        e.flow_unit = row[4]
        e.location = row[5]
        return e

    @staticmethod
    def index_of(file_path: str) -> List[FlowEntry]:
        index = []
        for (i, row) in _csv_rows_of(file_path):
            index.append(FlowEntry._from_csv(i, row))
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
    def _from_csv(index: int, row: List[str]) -> ImpactEntry:
        e = ImpactEntry()
        e.index = index
        e.impact_id = row[0]
        e.impact_name = row[1]
        e.impact_unit = row[2]
        return e

    @staticmethod
    def index_of(file_path: str) -> List[ImpactEntry]:
        index = []
        for (i, row) in _csv_rows_of(file_path):
            index.append(ImpactEntry._from_csv(i, row))
        return index


def matrix_of(file_path: str):
    if file_path.endswith('.npz'):
        return scipy.sparse.load_npz(file_path)
    return numpy.load(file_path)


def _csv_rows_of(f: str) -> Iterator[Tuple[int, List[str]]]:
    with open(f, 'r', encoding='utf-8') as stream:
        reader = csv.reader(stream)
        next(reader)  # skip header
        i = -1
        for row in reader:
            i += 1
            yield (i, row)


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


def solve(A, f):
    """Note that we currently convert sparse matrices to a dense format."""
    M = A
    if scipy.sparse.issparse(A):
        M = A.todense()
    return numpy.linalg.solve(M, f)


def invert(A):
    pass
