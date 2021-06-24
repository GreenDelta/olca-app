package org.openlca.app.editors.projects.reports.model;

import java.util.Objects;

import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.ImpactDescriptor;

class ReportIndicator {

  final ImpactDescriptor impact;
  final Double normalisationFactor;
  final Double weightingFactor;

  ReportIndicator(Project project, ImpactDescriptor impact) {
    this.impact = Objects.requireNonNull(impact);
    Double nf = null;
    Double wf = null;
    if (project != null && project.nwSet != null) {
      var f = project.nwSet.getFactor(impact);
      if (f != null) {
        nf = f.normalisationFactor;
        wf = f.weightingFactor;
      }
    }
    normalisationFactor = nf;
    weightingFactor = wf;
  }

  static ReportIndicator of (Project project, ImpactDescriptor impact) {
    return new ReportIndicator(project, impact);
  }

}
