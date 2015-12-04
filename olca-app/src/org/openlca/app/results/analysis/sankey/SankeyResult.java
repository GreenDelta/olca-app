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
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResultProvider;
import org.openlca.util.Doubles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SankeyResult {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem productSystem;
	private FullResultProvider results;

	private LongIndex processIndex;
	private ProcessDescriptor[] processes;
	private double[] upstreamResults;
	private double[] upstreamContributions;
	private double[] directResults;
	private double[] directContributions;

	public SankeyResult(ProductSystem productSystem,
			FullResultProvider results) {
		this.productSystem = productSystem;
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
		for (Long process : productSystem.getProcesses()) {
			double contr = getUpstreamContribution(process);
			if (Math.abs(contr) >= cutoff)
				processes.add(process);
		}
		return processes;
	}

	public double getLinkContribution(ProcessLink processLink) {
		if (processLink == null || results == null)
			return 0;
		double totalContr = getUpstreamContribution(processLink.getProviderId());
		double linkShare = results.getLinkShare(processLink);
		return totalContr * linkShare;
	}

	public void calculate(Object selection) {
		log.trace("Calculate Sankey result for selection {}", selection);
		buildProcessIndex();
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor f = (FlowDescriptor) selection;
			upstreamResults = vec(p -> results.getUpstreamFlowResult(p, f).value);
			directResults = vec(p -> results.getSingleFlowResult(p, f).value);
		} else if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor i = (ImpactCategoryDescriptor) selection;
			upstreamResults = vec(p -> results.getUpstreamImpactResult(p, i).value);
			directResults = vec(p -> results.getSingleImpactResult(p, i).value);
		} else if (selection instanceof CostResultDescriptor) {
			CostResultDescriptor c = (CostResultDescriptor) selection;
			upstreamResults = vec(p -> {
				double v = results.getUpstreamCostResult(p);
				return c.forAddedValue ? -v : v;
			});
			directResults = vec(p -> {
				double v = results.getSingleCostResult(p);
				return c.forAddedValue ? -v : v;
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

	private double[] vec(ToDoubleFunction<ProcessDescriptor> fn) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			ProcessDescriptor process = processes[i];
			double result = fn.applyAsDouble(process);
			vector[i] = result;
		}
		return vector;
	}

	private void buildProcessIndex() {
		processIndex = new LongIndex();
		Set<ProcessDescriptor> processSet = results.getProcessDescriptors();
		processes = new ProcessDescriptor[processSet.size()];
		for (ProcessDescriptor process : processSet) {
			int i = processIndex.put(process.getId());
			processes[i] = process;
		}
	}

}
