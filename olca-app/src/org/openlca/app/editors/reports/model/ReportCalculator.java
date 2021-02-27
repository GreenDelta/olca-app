package org.openlca.app.editors.reports.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.reports.model.ReportIndicatorResult.VariantResult;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportCalculator implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Project project;
	private final Report report;
	public boolean hadError = false;

	public ReportCalculator(Project project, Report report) {
		this.project = project;
		this.report = report;
	}

	@Override
	public void run() {
		if (project == null || report == null)
			return;
		report.results.clear();
		report.addedValues.clear();
		report.netCosts.clear();
		if (project.impactMethod == null)
			return;
		ProjectResult result;
		try {
			var calculator = new SystemCalculator(Database.get());
			result = calculator.calculate(project);
			hadError = false;
		} catch (OutOfMemoryError e) {
			MsgBox.error(M.OutOfMemory, M.CouldNotAllocateMemoryError);
			hadError = true;
			return;
		} catch (Exception e) {
			MsgBox.error("The calculation of the project failed "
					+ "with an unexpected error: " + e.getMessage()
					+ ". See the log file for further information.");
			log.error("Calculation of project failed", e);
			hadError = true;
			return;
		}
		appendResults(result);
		appendCostResults(result);
		if (project.nwSet != null) {
			appendNwFactors();
		}
	}

	private void appendNwFactors() {
		try {
			NwSetTable table = NwSetTable.of(
					Database.get(), project.nwSet);
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

	private void appendResults(ProjectResult result) {
		for (ImpactDescriptor impact : result.getImpacts()) {
			ReportIndicatorResult repResult = initReportResult(impact);
			if (repResult == null)
				continue; // should not add this indicator
			report.results.add(repResult);
			for (ProjectVariant variant : result.getVariants()) {
				VariantResult varResult = new VariantResult();
				repResult.variantResults.add(varResult);
				varResult.variant = variant.name;
				varResult.totalAmount = result.getTotalImpactResult(
						variant, impact);
				List<Contribution<CategorizedDescriptor>> set = result
						.getResult(variant)
						.getProcessContributions(impact);
				appendProcessContributions(set, varResult);
			}
		}
	}

	private ReportIndicatorResult initReportResult(ImpactDescriptor impact) {
		for (ReportIndicator indicator : report.indicators) {
			if (!indicator.displayed)
				continue;
			if (Objects.equals(impact, indicator.descriptor))
				return new ReportIndicatorResult(indicator.id);
		}
		return null;
	}

	private void appendProcessContributions(
			List<Contribution<CategorizedDescriptor>> contributions,
			VariantResult varResult) {
		Contribution<Long> rest = new Contribution<>();
		varResult.contributions.add(rest);
		rest.item = -1L;
		rest.isRest = true;
		rest.amount = 0;
		Set<Long> ids = getContributionProcessIds();
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

	private Set<Long> getContributionProcessIds() {
		Set<Long> ids = new TreeSet<>();
		for (ReportProcess process : report.processes) {
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

	private void appendCostResults(ProjectResult result) {
		if (result == null)
			return;
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
			CurrencyDao dao = new CurrencyDao(Database.get());
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
