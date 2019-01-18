package org.openlca.app.results.analysis.sankey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.openlca.app.util.CostResultDescriptor;
import org.openlca.core.matrix.LongIndex;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.util.Doubles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SankeyResult {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem system;
	private FullResult results;

	private LongIndex processIndex;
	private CategorizedDescriptor[] processes;
	private double[] upstreamResults;
	private double[] upstreamContributions;
	private double[] directResults;
	private double[] directContributions;

	public SankeyResult(ProductSystem system, FullResult results) {
		this.system = system;
		this.results = results;
	}

	public double getDirectResult(long processId) {
		return fetchVal(processId, directResults);
	}

	public double getDirectContribution(long processId) {
		return fetchVal(processId, directContributions);
	}

	public double getUpstreamResult(long processId) {
		return fetchVal(processId, upstreamResults);
	}

	public double getUpstreamContribution(long processId) {
		return fetchVal(processId, upstreamContributions);
	}

	private double fetchVal(long processId, double[] values) {
		if (values == null || processIndex == null)
			return Double.NaN;
		int idx = processIndex.getIndex(processId);
		if (idx < 0 || idx >= values.length)
			return Double.NaN;
		return values[idx];
	}

	public double findCutoff(int maxProcessesCount) {
		if (upstreamContributions.length == 0
				|| maxProcessesCount >= upstreamContributions.length)
			return 0;
		int length = upstreamContributions.length;
		double[] contributions = new double[length];
		System.arraycopy(upstreamContributions, 0, contributions, 0, length);
		Arrays.sort(contributions);
		return Math.abs(contributions[length - maxProcessesCount]);
	}

	public List<Long> getProcesseIdsAboveCutoff(double cutoff) {
		List<Long> processes = new ArrayList<>();
		for (Long process : system.processes) {
			double contr = getUpstreamContribution(process);
			if (Math.abs(contr) >= cutoff)
				processes.add(process);
		}
		return processes;
	}

	public double getLinkContribution(ProcessLink link) {
		if (link == null || results == null)
			return 0;
		double totalContr = getUpstreamContribution(link.providerId);
		double linkShare = results.getLinkShare(link);
		return totalContr * linkShare;
	}

	public void calculate(Object selection) {
		log.trace("Calculate Sankey result for selection {}", selection);
		buildProcessIndex();
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor f = (FlowDescriptor) selection;
			upstreamResults = vec(p -> results.getUpstreamFlowResult(p, f));
			directResults = vec(p -> results.getDirectFlowResult(p, f));
		} else if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor i = (ImpactCategoryDescriptor) selection;
			upstreamResults = vec(p -> results.getUpstreamImpactResult(p, i));
			directResults = vec(p -> results.getDirectImpactResult(p, i));
		} else if (selection instanceof CostResultDescriptor) {
			CostResultDescriptor c = (CostResultDescriptor) selection;
			upstreamResults = vec(p -> {
				double v = results.getUpstreamCostResult(p);
				return c.forAddedValue && v != 0 ? -v : v;
			});
			directResults = vec(p -> {
				double v = results.getDirectCostResult(p);
				return c.forAddedValue && v != 0 ? -v : v;
			});
		} else {
			directResults = upstreamResults = new double[processIndex.size()];
		}
		upstreamContributions = calcContributions(upstreamResults);
		directContributions = calcContributions(directResults);
		log.trace("Calculation done");
	}

	private double[] calcContributions(double[] values) {
		if (values == null || values.length == 0)
			return new double[0];
		double min = Doubles.min(values);
		double max = Doubles.max(values);
		double ref = Math.max(Math.abs(min), Math.abs(max));
		double[] contributions = new double[values.length];
		if (ref == 0)
			return contributions;
		for (int i = 0; i < contributions.length; i++) {
			double val = values[i];
			contributions[i] = val / ref;
		}
		return contributions;
	}

	private double[] vec(ToDoubleFunction<CategorizedDescriptor> fn) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			CategorizedDescriptor process = processes[i];
			double result = fn.applyAsDouble(process);
			vector[i] = result;
		}
		return vector;
	}

	private void buildProcessIndex() {
		processIndex = new LongIndex();
		Set<CategorizedDescriptor> processSet = results.getProcesses();
		processes = new CategorizedDescriptor[processSet.size()];
		for (CategorizedDescriptor process : processSet) {
			int i = processIndex.put(process.id);
			processes[i] = process;
		}
	}

}
