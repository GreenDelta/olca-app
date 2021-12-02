package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;

class TreeContentProvider implements ITreeContentProvider {

	private final ContributionResult result;
	private final LocationPage page;

	public TreeContentProvider(LocationPage page) {
		this.page = page;
		this.result = page.result;
	}

	@Override
	public Object[] getElements(Object obj) {
		if (obj == null)
			return new Object[0];
		if (obj instanceof Object[])
			return (Object[]) obj;
		if (obj instanceof Collection) {
			Collection<?> coll = (Collection<?>) obj;
			return coll.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object obj) {
		if (!(obj instanceof Contribution))
			return null;
		Contribution<?> c = (Contribution<?>) obj;

		// calculated contributions are cached
		if (c.childs != null && !c.childs.isEmpty())
			return c.childs.toArray();

		// leaf of the contribution tree
		if (!(c.item instanceof Location))
			return null;

		Location loc = (Location) c.item;
		Object selection = page.getSelection();
		Stream<Contribution<?>> stream = null;
		if (selection instanceof FlowDescriptor) {
			stream = contributions(loc, (FlowDescriptor) selection);
		} else if (selection instanceof CostResultDescriptor) {
			stream = contributions(loc, (CostResultDescriptor) selection);
		} else if (selection instanceof ImpactDescriptor) {
			stream = contributions(loc, (ImpactDescriptor) selection);
		}

		if (stream == null)
			return null;

		// TODO apply cutoff
		c.childs = stream
				.filter(con -> con.amount != 0)
				.sorted((c1, c2) -> Double.compare(c2.amount, c1.amount))
				.collect(Collectors.toList());
		return c.childs.toArray();
	}

	@Override
	public Object getParent(Object obj) {
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		if (!(obj instanceof Contribution))
			return false;
		Contribution<?> c = (Contribution<?>) obj;
		if (c.childs != null && !c.childs.isEmpty())
			return false;
		return c.item instanceof Location;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(
			Viewer viewer, Object old, Object newInput) {
	}

	private Stream<Contribution<?>> contributions(
			Location loc, FlowDescriptor flow) {

		// get the matrix row => IndexFlow
		var enviIndex = result.enviIndex();
		int idx = enviIndex.isRegionalized()
				? enviIndex.of(flow.id, loc.id)
				: enviIndex.of(flow.id);
		if (idx < 0)
			return null;
		var enviFlow = enviIndex.at(idx);
		if (enviFlow == null)
			return null;

		// prepare the stream
		double total = result.getTotalFlowResult(enviFlow);

		// in a regionalized result, the flow with the
		// given location can occur in processes that
		// have another location which is not the case
		// in a non-regionalized result
		if (enviIndex.isRegionalized()) {
			return result.techIndex().content().stream().map(p -> {
				var c = Contribution.of(p.provider());
				c.amount = result.getDirectFlowResult(p, enviFlow);
				c.computeShare(total);
				return c;
			});
		} else {
			return processes(loc).stream().map(p -> {
				var c = Contribution.of(p);
				c.amount = result.getDirectFlowResult(p, enviFlow);
				c.computeShare(total);
				return c;
			});
		}
	}

	private Stream<Contribution<?>> contributions(
			Location loc, CostResultDescriptor c) {
		double total = c.forAddedValue
				? -result.totalCosts()
				: result.totalCosts();
		return processes(loc).stream().map(p -> {
			Contribution<?> con = Contribution.of(p);
			con.amount = c.forAddedValue
					? -result.getDirectCostResult(p)
					: result.getDirectCostResult(p);
			con.computeShare(total);
			return con;
		});
	}

	private Stream<Contribution<?>> contributions(
			Location loc, ImpactDescriptor impact) {

		double total = result.getTotalImpactResult(impact);
		if (!result.enviIndex().isRegionalized()) {
			return processes(loc).stream().map(p -> {
				Contribution<?> c = Contribution.of(p);
				c.amount = result.getDirectImpactResult(p, impact);
				c.computeShare(total);
				return c;
			});
		}

		// first collect all flows in that location
		var flows = new ArrayList<EnviFlow>();
		for(var enviFlow : result.enviIndex()) {
			if (loc == null && enviFlow.location() == null) {
				flows.add(enviFlow);
				continue;
			}
			if (loc == null || enviFlow.location() == null)
				continue;
			if (loc.id == enviFlow.location().id) {
				flows.add(enviFlow);
			}
		}

		return flows.stream().map(iFlow -> {
			Contribution<?> c = Contribution.of(iFlow);
			c.amount = result.getDirectFlowImpact(iFlow, impact);
			c.computeShare(total);
			return c;
		});
	}

	private List<ProcessDescriptor> processes(Location loc) {
		List<ProcessDescriptor> list = new ArrayList<>();
		result.techIndex().each((i, product) -> {
			if (!(product.provider() instanceof ProcessDescriptor))
				return;
			var process = (ProcessDescriptor) product.provider();
			if (loc == null && process.location == null) {
				list.add(process);
				return;
			}
			if (loc == null || process.location == null)
				return;
			if (loc.id == process.location) {
				list.add(process);
			}
		});
		return list;
	}

}