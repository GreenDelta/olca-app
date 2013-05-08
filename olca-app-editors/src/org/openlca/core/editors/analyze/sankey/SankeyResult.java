package org.openlca.core.editors.analyze.sankey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.math.Index;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.util.Doubles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Result for the Sankey diagram.
 */
class SankeyResult {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem productSystem;
	private AnalysisResult results;

	private Index<Process> processIndex;
	private double[] totalResults;
	private double[] totalContributions;
	private double[] singleResults;
	private double[] singleContributions;
	private Map<String, Double> linkContribution;

	public SankeyResult(ProductSystem productSystem, AnalysisResult results) {
		this.productSystem = productSystem;
		this.results = results;
	}

	public double getSingleResult(Process process) {
		return fetchVal(process, singleResults);
	}

	public double getSingleContribution(Process process) {
		return fetchVal(process, singleContributions);
	}

	public double getTotalResult(Process process) {
		return fetchVal(process, totalResults);
	}

	public double getTotalContribution(Process process) {
		return fetchVal(process, totalContributions);
	}

	private double fetchVal(Process process, double[] values) {
		if (process == null || values == null || processIndex == null)
			return Double.NaN;
		int idx = processIndex.getIndex(process);
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

	public List<String> getProcesseIdsAboveCutoff(double cutoff) {
		List<String> processes = new ArrayList<>();
		for (Process process : productSystem.getProcesses()) {
			double contr = getTotalContribution(process);
			if (Math.abs(contr) >= cutoff)
				processes.add(process.getId());
		}
		return processes;
	}

	public double getLinkContribution(ProcessLink processLink) {
		if (processLink == null || linkContribution == null)
			return 0;
		Double ratio = linkContribution.get(processLink.getId());
		if (ratio == null)
			return 0;
		return ratio;
	}

	/** Calculates the results for the given selection and cutoff. */
	public void calculate(Object selection) {
		log.trace("Calculating sankey result");
		buildProcessIndex();
		calc(selection);
		calcLinkContributions();
	}

	private void calc(Object selection) {
		log.trace("Calculate for selection {}", selection);
		if (selection instanceof Flow) {
			totalResults = calcAggFlowResults((Flow) selection);
			singleResults = calcSingFlowResults((Flow) selection);
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

	private double[] calcAggFlowResults(Flow flow) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			Process process = processIndex.getItemAt(i);
			double result = results.getResult(process, flow);
			vector[i] = result;
		}
		return vector;
	}

	private double[] calcSingFlowResults(Flow flow) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			Process process = processIndex.getItemAt(i);
			double result = results.getSingleResult(process, flow);
			vector[i] = result;
		}
		return vector;
	}

	private double[] calcAggImpactResults(ImpactCategoryDescriptor category) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			Process process = processIndex.getItemAt(i);
			double result = results.getResult(process, category);
			vector[i] = result;
		}
		return vector;
	}

	private double[] calcSingImpactResults(ImpactCategoryDescriptor category) {
		double[] vector = new double[processIndex.size()];
		for (int i = 0; i < processIndex.size(); i++) {
			Process process = processIndex.getItemAt(i);
			double result = results.getSingleResult(process, category);
			vector[i] = result;
		}
		return vector;
	}

	private void buildProcessIndex() {
		processIndex = new Index<>(Process.class);
		for (Process process : productSystem.getProcesses())
			processIndex.put(process);
	}

	private void calcLinkContributions() {
		log.trace("Calculate link contributions");
		linkContribution = new HashMap<>();
		for (Process provider : productSystem.getProcesses()) {
			double providerContribution = getTotalContribution(provider);
			if (providerContribution == 0)
				continue;
			ProcessLink[] links = productSystem.getOutgoingLinks(provider
					.getId());
			if (links == null || links.length == 0)
				continue;
			if (links.length == 1) {
				linkContribution.put(links[0].getId(), providerContribution);
				continue;
			}
			double[] recipientFactors = calcRecipientFactors(links);
			for (int i = 0; i < recipientFactors.length; i++) {
				double contr = providerContribution * recipientFactors[i];
				linkContribution.put(links[i].getId(), contr);
			}
		}
		log.trace("Calculation of link contributions done");
	}

	private double[] calcRecipientFactors(ProcessLink[] links) {
		double[] recipientAmounts = new double[links.length];
		for (int i = 0; i < links.length; i++) {
			Process recipient = links[i].getRecipientProcess();
			double scaling = results.getScalingFactor(recipient);
			double amount = links[i].getRecipientInput().getConvertedResult();
			recipientAmounts[i] = scaling * amount;
		}
		double sum = Doubles.sum(recipientAmounts);
		double[] linkFactors = new double[links.length];
		for (int i = 0; i < linkFactors.length; i++)
			linkFactors[i] = recipientAmounts[i] / sum;
		return linkFactors;
	}
}
