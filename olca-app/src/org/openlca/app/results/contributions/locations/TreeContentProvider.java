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
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;

class TreeContentProvider implements ITreeContentProvider {

	private final LcaResult result;
	private final LocationPage page;

	public TreeContentProvider(LocationPage page) {
		this.page = page;
		this.result = page.editor.result;
	}

	@Override
	public Object[] getElements(Object obj) {
		if (obj == null)
			return new Object[0];
		if (obj instanceof Object[])
			return (Object[]) obj;
		if (obj instanceof Collection<?> coll) {
			return coll.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object obj) {
		if (!(obj instanceof Contribution<?> c))
			return null;

		// calculated contributions are cached
		if (c.childs != null && !c.childs.isEmpty())
			return c.childs.toArray();

		// leaf of the contribution tree
		if (!(c.item instanceof Location loc))
			return null;
		double total = c.amount;

		Object selection = page.getSelection();
		Stream<Contribution<?>> stream = null;
		if (selection instanceof FlowDescriptor flow) {
			stream = contributions(loc, total, flow);
		} else if (selection instanceof CostResultDescriptor costs) {
			stream = contributions(loc, total, costs);
		} else if (selection instanceof ImpactDescriptor impact) {
			stream = contributions(loc, total, impact);
		}

		if (stream == null)
			return null;

		c.childs = stream
				.filter(page::applyFilter)
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
		if (!(obj instanceof Contribution<?> c))
			return false;
		if (c.childs != null && !c.childs.isEmpty())
			return false;
		return c.item instanceof Location;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object old, Object newInput) {
	}

	private Stream<Contribution<?>> contributions(
			Location loc, double total, FlowDescriptor flow
	) {
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

		// in a regionalized result, the flow with the
		// given location can occur in processes that
		// have another location which is not the case
		// in a non-regionalized result
		if (enviIndex.isRegionalized()) {
			return result.techIndex().stream().map(techFlow -> {
				var c = Contribution.of(techFlow);
				c.amount = result.getDirectFlowOf(enviFlow, techFlow);
				c.computeShare(total);
				return c;
			});
		} else {
			return techFlows(loc).stream().map(techFlow -> {
				var c = Contribution.of(techFlow);
				c.amount = result.getDirectFlowOf(enviFlow, techFlow);
				c.computeShare(total);
				return c;
			});
		}
	}

	private Stream<Contribution<?>> contributions(
			Location loc, double total, CostResultDescriptor c
	) {
		return techFlows(loc).stream().map(p -> {
			var con = Contribution.of(p);
			con.amount = c.forAddedValue
					? -result.getDirectCostsOf(p)
					: result.getDirectCostsOf(p);
			con.computeShare(total);
			return con;
		});
	}

	private Stream<Contribution<?>> contributions(
			Location loc, double total, ImpactDescriptor impact
	) {
		if (!result.enviIndex().isRegionalized()) {
			return techFlows(loc)
					.stream()
					.map(techFlow -> {
				var c = Contribution.of(techFlow);
				c.amount = result.getDirectImpactOf(impact, techFlow);
				c.computeShare(total);
				return c;
			});
		}

		// first collect all flows in that location
		var flows = new ArrayList<EnviFlow>();
		for (var enviFlow : result.enviIndex()) {
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

		return flows.stream().map(enviFlow -> {
			var c = Contribution.of(enviFlow);
			c.amount = result.getFlowImpactOf(impact, enviFlow);
			c.computeShare(total);
			return c;
		});
	}

	private List<TechFlow> techFlows(Location loc) {
		List<TechFlow> list = new ArrayList<>();
		result.techIndex().each((i, techFlow) -> {
			if (!(techFlow.provider() instanceof ProcessDescriptor process))
				return;
			if (loc == null && process.location == null) {
				list.add(techFlow);
				return;
			}
			if (loc == null || process.location == null)
				return;
			if (loc.id == process.location) {
				list.add(techFlow);
			}
		});
		return list;
	}

}
