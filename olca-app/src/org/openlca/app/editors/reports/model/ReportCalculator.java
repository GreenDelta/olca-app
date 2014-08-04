package org.openlca.app.editors.reports.model;

import java.util.Objects;

import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.reports.model.ReportResult.Contribution;
import org.openlca.app.editors.reports.model.ReportResult.VariantResult;
import org.openlca.app.util.Labels;
import org.openlca.core.math.ProjectCalculator;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.ImpactResult;
import org.openlca.core.results.ProjectResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportCalculator implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Project project;
	private final Report report;

	public ReportCalculator(Project project, Report report) {
		this.project = project;
		this.report = report;
	}

	@Override
	public void run() {
		if (project == null || report == null)
			return;
		report.getResults().clear();
		if (project.getImpactMethodId() == null)
			return;
		ProjectResultProvider projectResult = calcProject(project);
		if (projectResult == null)
			return;
		appendResults(projectResult);
	}

	private void appendResults(ProjectResultProvider result) {
		for (ImpactCategoryDescriptor impact : result.getImpactDescriptors()) {
			ReportResult repResult = initReportResult(impact);
			if (repResult == null)
				continue; // should not add this indicator
			report.getResults().add(repResult);
			for (ProjectVariant variant : result.getVariants()) {
				VariantResult varResult = new VariantResult();
				repResult.getVariantResults().add(varResult);
				varResult.setVariant(variant.getName());
				ImpactResult impactResult = result.getTotalImpactResult(
						variant, impact);
				varResult.setTotalAmount(impactResult.getValue());
				ContributionSet<ProcessDescriptor> set = result
						.getResult(variant)
						.getProcessContributions(impact);
				Contributions.topWithRest(set.getContributions(), 5)
						.forEach((item) -> addContribution(item, varResult));
			}
		}
	}

	private ReportResult initReportResult(ImpactCategoryDescriptor impact) {
		for (ReportIndicator indicator : report.getIndicators()) {
			if (!indicator.isDisplayed())
				continue;
			if (Objects.equals(impact, indicator.getDescriptor()))
				return new ReportResult(indicator.getId());
		}
		return null;
	}

	private void addContribution(ContributionItem<ProcessDescriptor> item,
			VariantResult varResult) {
		Contribution contribution = new Contribution();
		varResult.getContributions().add(contribution);
		contribution.setAmount(item.getAmount());
		if (item.isRest()) {
			contribution.setProcess(Messages.Other);
			contribution.setRest(true);
		} else {
			String name = Labels.getDisplayName(item.getItem());
			contribution.setProcess(name);
			contribution.setRest(false);
		}
	}

	private ProjectResultProvider calcProject(Project project) {
		try {
			ProjectCalculator calculator = new ProjectCalculator(
					Cache.getMatrixCache(), App.getSolver());
			return calculator.solve(project, Cache.getEntityCache());
		} catch (Exception e) {
			log.error("Calculation of project failed");
			return null;
		}
	}
}
