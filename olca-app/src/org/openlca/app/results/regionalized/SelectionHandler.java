package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.geo.RegionalizedResultProvider;
import org.openlca.geo.kml.KmlLoadResult;

abstract class SelectionHandler implements EventHandler {

	private RegionalizedResultProvider result;

	SelectionHandler(RegionalizedResultProvider result) {
		this.result = result;
	}

	protected abstract void processResultData(List<LocationResult> results);

	@Override
	public void flowSelected(FlowDescriptor flow) {
		ContributionResultProvider<?> provider = result.getRegionalizedResult();
		Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
		Map<Long, Double> results = new HashMap<>();
		for (ProcessDescriptor process : processes) {
			double amount = provider.getSingleFlowResult(process, flow)
					.getValue();
			results.put(process.getId(), amount);
		}
		processResultData(getResultData(results));
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		ContributionResultProvider<?> provider = result.getRegionalizedResult();
		Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
		Map<Long, Double> results = new HashMap<>();
		for (ProcessDescriptor process : processes) {
			double amount = provider.getSingleImpactResult(process, impact)
					.getValue();
			results.put(process.getId(), amount);
		}
		processResultData(getResultData(results));
	}

	private List<LocationResult> getResultData(Map<Long, Double> results) {
		List<KmlLoadResult> kmlData = result.getKmlData();
		List<LocationResult> locationResults = new ArrayList<>();
		for (KmlLoadResult data : kmlData) {
			LocationResult result = new LocationResult(data.getKmlFeature(),
					data.getLocationId());
			for (LongPair processProduct : data.getProcessProducts())
				result.addAmount(results.get(processProduct.getFirst()));
			locationResults.add(result);
		}
		return locationResults;
	}

}