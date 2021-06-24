package org.openlca.app.editors.projects.reports.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.openlca.app.editors.projects.results.ProjectResultData;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

class ReportFiller {

  private final ProjectResultData data;
  private final IDatabase db;
  private final Project project;
  private final ProjectResult result;
  private final ResultItemView items;

  private ReportFiller(ProjectResultData data) {
    this.data = data;
    this.db = data.db();
    this.project = data.project();
    this.result = data.result();
    this.items = data.items();
  }

  static ReportFiller of(ProjectResultData data) {
    return new ReportFiller(data);
  }

  void fill(Report report) {
    if (report == null)
      return;
    report.clearResults();

    // add project variants & parameters
    for (var variant : project.variants) {
      if (variant.isDisabled)
        continue;
      report.variants.add(ReportVariant.of(variant));
    }
    report.parameters.addAll(
      ReportParameter.allOf(db, data.project()));

    // add cost results
    if (result.hasCosts()) {
      appendCostResults(report);
    }

    // add impacts
    if (project.impactMethod == null)
      return;

    // add the impact categories
    for (var impact : items.impacts()) {
      report.indicators.add(ReportIndicator.of(project, impact));
    }

    // the report may was loaded from disk and, thus, we cannot be sure that the
    // IDs of the process descriptors are in sync with the database. Note that
    // this has to be done BEFORE we query the result contributions
    if (!report.processes.isEmpty()) {
      var ids = report.processes.stream()
        .map(p -> p.refId)
        .collect(Collectors.toSet());
      var synced = new ArrayList<ProcessDescriptor>();
      for (var tf : items.techFlows()) {
        var p = tf.process();
        if (ids.contains(p.refId) && p instanceof ProcessDescriptor) {
          synced.add((ProcessDescriptor) p);
        }
      }
      report.processes.clear();
      report.processes.addAll(synced);
    }

    report.results.addAll(ReportImpactResult.allOf(report, data));

  }

  private void appendCostResults(Report report) {

    var currency = new CurrencyDao(db).getReferenceCurrency();
    for (var v : result.getVariants()) {
      double costs = result.getResult(v).totalCosts;
      report.netCosts.add(ReportCostResult.of(v, currency, costs));
      double addedValue = costs == 0 ? 0 : -costs;
      report.addedValues.add(ReportCostResult.of(v, currency, addedValue));
    }
    Comparator<ReportCostResult> c =
      (r1, r2) -> Strings.compare(r1.variant, r2.variant);
    report.netCosts.sort(c);
    report.addedValues.sort(c);
  }

}
