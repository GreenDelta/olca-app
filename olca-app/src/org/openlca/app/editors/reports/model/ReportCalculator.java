package org.openlca.app.editors.reports.model;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.reports.model.ReportResult.Contribution;
import org.openlca.app.editors.reports.model.ReportResult.VariantResult;
import org.openlca.core.math.ProjectCalculator;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionSet;
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
		if (project.getNwSetId() != null)
			appendNwFactors();
	}

	private ProjectResultProvider calcProject(Project project) {
		try {
			ProjectCalculator calculator = new ProjectCalculator(
					Cache.getMatrixCache(), App.getSolver());
			return calculator.solve(project, Cache.getEntityCache());
		} catch (Exception e) {
			log.error("Calculation of project failed", e);
			return null;
		}
	}

	private void appendNwFactors() {
		try {
			NwSetTable table = NwSetTable.build(Database.get(),
					project.getNwSetId());
			report.setWithNormalisation(table.hasNormalisationFactors());
			report.setWithWeighting(table.hasWeightingFactors());
			for (ReportIndicator indicator : report.getIndicators()) {
				if (indicator.getDescriptor() == null)
					continue;
				long categoryId = indicator.getDescriptor().getId();
				if (table.hasNormalisationFactors()) {
					double nf = table.getNormalisationFactor(categoryId);
					indicator.setNormalisationFactor(nf);
				}
				if (table.hasWeightingFactors()) {
					double wf = table.getWeightingFactor(categoryId);
					indicator.setWeightingFactor(wf);
				}
			}
		} catch (Exception e) {
			log.error("failed to load normalisation/weighting factors", e);
		}
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
				appendProcessContributions(set, varResult);
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

	private void appendProcessContributions(
			ContributionSet<ProcessDescriptor> set, VariantResult varResult) {
		Contribution rest = new Contribution();
		varResult.getContributions().add(rest);
		rest.setRest(true);
		rest.setProcessId(-1);
		rest.setAmount(0);
		Set<Long> ids = getContributionProcessIds();
		Set<Long> foundIds = new TreeSet<>();
		for (ContributionItem<ProcessDescriptor> item : set.getContributions()) {
			if (item.getItem() == null)
				continue;
			if (!ids.contains(item.getItem().getId()))
				rest.setAmount(rest.getAmount() + item.getAmount());
			else {
				foundIds.add(item.getItem().getId());
				addContribution(varResult, item);
			}
		}
		addDefaultContributions(ids, foundIds, varResult);
	}

	private void addContribution(VariantResult varResult,
			ContributionItem<ProcessDescriptor> item) {
		Contribution con = new Contribution();
		varResult.getContributions().add(con);
		con.setAmount(item.getAmount());
		con.setRest(false);
		con.setProcessId(item.getItem().getId());
	}

	private Set<Long> getContributionProcessIds() {
		Set<Long> ids = new TreeSet<>();
		for (ReportProcess process : report.getProcesses()) {
			if (process.getDescriptor() == null)
				continue;
			ids.add(process.getDescriptor().getId());
		}
		return ids;
	}

	/**
	 * Add zero-contributions for processes that were not found in a variant
	 * result.
	 */
	private void addDefaultContributions(Set<Long> ids, Set<Long> foundIds,
			VariantResult varResult) {
		TreeSet<Long> notFound = new TreeSet<>(ids);
		notFound.removeAll(foundIds);
		for (long id : notFound) {
			Contribution con = new Contribution();
			varResult.getContributions().add(con);
			con.setAmount(0);
			con.setRest(false);
			con.setProcessId(id);
		}
	}
}
