package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.geo.RegionalizedResult;
import org.openlca.geo.kml.LocationKml;

abstract class SelectionHandler implements EventHandler {

	private RegionalizedResult result;

	SelectionHandler(RegionalizedResult result) {
		this.result = result;
	}

	protected abstract void processResultData(List<LocationResult> results);

	@Override
	public void flowSelected(FlowDescriptor flow) {
		ContributionResult provider = result.result;
		Set<CategorizedDescriptor> processes = provider.getProcesses();
		Map<Long, Double> results = new HashMap<>();
		for (CategorizedDescriptor process : processes) {
			double v = provider.getDirectFlowResult(process, flow);
			results.put(process.id, v);
		}
		processResultData(getResultData(results));
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		ContributionResult provider = result.result;
		Set<CategorizedDescriptor> processes = provider.getProcesses();
		Map<Long, Double> results = new HashMap<>();
		for (CategorizedDescriptor process : processes) {
			double v = provider.getDirectImpactResult(process, impact);
			results.put(process.id, v);
		}
		processResultData(getResultData(results));
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		ContributionResult provider = result.result;
		Set<CategorizedDescriptor> processes = provider.getProcesses();
		Map<Long, Double> results = new HashMap<>();
		for (CategorizedDescriptor process : processes) {
			double v = provider.getDirectCostResult(process);
			v = cost.forAddedValue ? -v : v;
			results.put(process.id, v);
		}
		processResultData(getResultData(results));
	}

	private List<LocationResult> getResultData(Map<Long, Double> results) {
		List<LocationResult> list = new ArrayList<>();
		for (LocationKml data : result.kmlData) {
			LocationResult result = new LocationResult(data.kmlFeature,
					data.locationId);
			for (ProcessProduct product : data.processProducts)
				result.amount += results.get(product.id());
			list.add(result);
		}
		return list;
	}

}