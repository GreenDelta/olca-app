package org.openlca.app.analysis.sankey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

/**
 * Result for the Sankey diagram.
 */
class SankeyResult {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem productSystem;
	private FullResultProvider results;

	private LongIndex processIndex;
	private ProcessDescriptor[] processes;
	private double[] totalResults;
	private double[] totalContributions;
	private double[] singleResults;
	private double[] singleContributions;

	public SankeyResult(ProductSystem productSystem, FullResultProvider results) {
		this.productSystem = productSystem;
		this.results = results;
	}

	public double getSingleResult(long processId) {
		return fetchVal(processId, singleResults);
	}

	public double getSingleContribution(long processId) {
		return fetchVal(processId, singleContributions);
	}

	public double getTotalResult(long processId) {
		return fetchVal(processId, totalResults);
	}

	public double getTotalContribution(long processId) {
		return fetchVal(processId, totalContributions);
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
		if (totalContributions.length == 0
				|| maxProcessesCount >= totalContributions.length)
			return 0;
		int length = totalContributions.length;
		double[] contributions = new double[length];
		System.arraycopy(totalContributions, 0, contributions, 0, length);
		Arrays.sort(contributions);
		return Math.abs(contributions[length - maxProcessesCount]);
	}

	public List<Long> getProcesseIdsAboveCutoff(double cutoff) {
		List<Long> processes = new ArrayList<>();
		for (Long process : productSystem.getProcesses()) {
			double contr = getTotalContribution(process);
			if (Math.abs(contr) >= cutoff)
				processes.add(process);
		}
		return processes;
	}

	public double getLinkContribution(ProcessLink processLink) {
		if (processLink == null || results == null)
			return 0;
		double totalContr = getTotalContribution(processLink.getProviderId());
		double linkShare = results.getLinkShare(processLink);
		return totalContr * linkShare;
	}

	/** Calculates the results for the given selection and cutoff. */
	public void calculate(Object selection) {
		log.trace("Calculating sankey result");
		buildProcessIndex();
		calc(selection);
	}

	private void calc(Object selection) {
		log.trace("Calculate for selection {}", selection);
		if (selection instanceof FlowDescriptor) {
			totalResults = calcAggFlowResults((FlowDescriptor) selection);
			singleResults = calcSingFlowResults((FlowDescriptor) selection);
		} else if (selection instanceof ImpactCategoryDescriptor) {
			totalResults = calcAggImpactResults((ImpactCategoryDescriptor) selection);
			singleResults = calcSingImpactResults((ImpactCategoryDescriptor) selection);
		} else {
			singleResults = totalResults = new double[processIndex.size()];
		}
		totalContributions = calcContributions(totalResults);
		singleContributions = calcContributions(singleResults);
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

	private double[] calcAggFlowResults(FlowDescriptor flow) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			ProcessDescriptor process = processes[i];
			double result = results.getUpstreamFlowResult(process, flow)
					.getValue();
			vector[i] = result;
		}
		return vector;
	}

	private double[] calcSingFlowResults(FlowDescriptor flow) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			ProcessDescriptor process = processes[i];
			double result = results.getSingleFlowResult(process, flow)
					.getValue();
			vector[i] = result;
		}
		return vector;
	}

	private double[] calcAggImpactResults(ImpactCategoryDescriptor impact) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			ProcessDescriptor process = processes[i];
			double result = results.getUpstreamImpactResult(process, impact)
					.getValue();
			vector[i] = result;
		}
		return vector;
	}

	private double[] calcSingImpactResults(ImpactCategoryDescriptor impact) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			ProcessDescriptor process = processes[i];
			double result = results.getSingleImpactResult(process, impact)
					.getValue();
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
