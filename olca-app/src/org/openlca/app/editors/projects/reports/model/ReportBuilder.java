package org.openlca.app.editors.projects.reports.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.openlca.app.editors.projects.reports.model.ReportIndicatorResult.VariantResult;
import org.openlca.app.editors.projects.results.ProjectResultData;
import org.openlca.app.util.Numbers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportBuilder {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final IDatabase db;
	private final Project project;
	private final ProjectResult result;
	private final ResultItemView items;

	private ReportBuilder(ProjectResultData data) {
		this.db = data.db();
		this.project = data.project();
		this.result = data.result();
		this.items = data.items();
	}

	public static ReportBuilder of(ProjectResultData data) {
		return new ReportBuilder(data);
	}

	public void fill(Report report) {
		report.results.clear();
		report.addedValues.clear();
		report.netCosts.clear();
		if (project.impactMethod == null)
			return;
		appendResults(report);
		appendCostResults(report);
		if (project.nwSet != null) {
			appendNwFactors(report);
		}
	}

	private void appendNwFactors(Report report) {
		try {
			var table = NwSetTable.of(db, project.nwSet);
			report.withNormalisation = table.hasNormalization();
			report.withWeighting = table.hasWeighting();
			for (ReportIndicator indicator : report.indicators) {
				if (indicator.descriptor == null)
					continue;
				long categoryId = indicator.descriptor.id;
				if (table.hasNormalization()) {
					indicator.normalisationFactor =
							table.getNormalizationFactor(categoryId);
				}
				if (table.hasWeighting()) {
					indicator.weightingFactor =
							table.getWeightingFactor(categoryId);
				}
			}
		} catch (Exception e) {
			log.error("failed to load normalisation/weighting factors", e);
		}
	}

	private void appendResults(Report report) {
		for (var impact : items.impacts()) {
			var repResult = initReportResult(report, impact);
			if (repResult == null)
				continue; // should not add this indicator
			report.results.add(repResult);
			for (var variant : result.getVariants()) {
				var varResult = new VariantResult();
				repResult.variantResults.add(varResult);
				varResult.variant = variant.name;
				varResult.totalAmount = result.getTotalImpactResult(
						variant, impact);
				List<Contribution<CategorizedDescriptor>> set = result
						.getResult(variant)
						.getProcessContributions(impact);
				appendProcessContributions(report, set, varResult);
			}
		}
	}

	private ReportIndicatorResult initReportResult(
		Report report, ImpactDescriptor impact) {
		for (var indicator : report.indicators) {
			if (!indicator.displayed)
				continue;
			if (Objects.equals(impact, indicator.descriptor))
				return new ReportIndicatorResult(indicator.id);
		}
		return null;
	}

	private void appendProcessContributions(
			Report report,
			List<Contribution<CategorizedDescriptor>> contributions,
			VariantResult varResult) {
		Contribution<Long> rest = new Contribution<>();
		varResult.contributions.add(rest);
		rest.item = -1L;
		rest.isRest = true;
		rest.amount = 0;
		Set<Long> ids = getContributionProcessIds(report);
		Set<Long> foundIds = new TreeSet<>();
		for (Contribution<CategorizedDescriptor> item : contributions) {
			if (item.item == null)
				continue;
			if (!ids.contains(item.item.id))
				rest.amount = rest.amount + item.amount;
			else {
				foundIds.add(item.item.id);
				addContribution(varResult, item);
			}
		}
		addDefaultContributions(ids, foundIds, varResult);
	}

	private void addContribution(VariantResult varResult,
								 Contribution<CategorizedDescriptor> item) {
		Contribution<Long> con = new Contribution<>();
		varResult.contributions.add(con);
		con.amount = item.amount;
		con.isRest = false;
		con.item = item.item.id;
	}

	private Set<Long> getContributionProcessIds(Report report) {
		Set<Long> ids = new TreeSet<>();
		for (var process : report.processes) {
			ids.add(process.descriptor.id);
		}
		return ids;
	}

	/**
	 * Add zero-contributions for processes that were not found in a variant result.
	 */
	private void addDefaultContributions(Set<Long> ids, Set<Long> foundIds,
										 VariantResult varResult) {
		TreeSet<Long> notFound = new TreeSet<>(ids);
		notFound.removeAll(foundIds);
		for (long id : notFound) {
			Contribution<Long> con = new Contribution<>();
			varResult.contributions.add(con);
			con.amount = 0;
			con.isRest = false;
			con.item = id;
		}
	}

	private void appendCostResults(Report report) {
		String currency = getCurrency();
		for (ProjectVariant var : result.getVariants()) {
			double costs = result.getResult(var).totalCosts;
			report.netCosts.add(cost(var, costs, currency));
			double addedValue = costs == 0 ? 0 : -costs;
			report.addedValues.add(cost(var, addedValue, currency));
		}
		Comparator<ReportCostResult> c =
				(r1, r2) -> Strings.compare(r1.variant, r2.variant);
		report.netCosts.sort(c);
		report.addedValues.sort(c);
	}

	private ReportCostResult cost(ProjectVariant var, double val, String cu) {
		ReportCostResult r = new ReportCostResult();
		r.variant = var.name;
		r.value = Numbers.decimalFormat(val, 2) + " " + cu;
		return r;
	}

	private String getCurrency() {
		try {
			CurrencyDao dao = new CurrencyDao(db);
			Currency c = dao.getReferenceCurrency();
			if (c == null)
				return "?";
			return c.code != null ? c.code : c.name;
		} catch (Exception e) {
			log.error("failed to load default currency", e);
			return "?";
		}
	}
}
