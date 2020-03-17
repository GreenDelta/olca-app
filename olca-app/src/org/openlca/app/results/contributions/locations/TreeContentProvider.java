package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
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
		if (selection instanceof FlowDescriptor) {
			List<Contribution<?>> cons = contributions(
					loc, (FlowDescriptor) selection);
			c.childs = cons;
			return cons.toArray();
		}

		return null;
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

	private List<Contribution<?>> contributions(
			Location loc, FlowDescriptor flow) {

		// get the matrix row => IndexFlow
		FlowIndex flowIndex = result.flowIndex;
		int idx = flowIndex.isRegionalized
				? flowIndex.of(flow.id, loc.id)
				: flowIndex.of(flow.id);
		if (idx < 0)
			return Collections.emptyList();
		IndexFlow iFlow = flowIndex.at(idx);
		if (iFlow == null)
			return Collections.emptyList();

		// prepare the stream
		double total = result.getTotalFlowResult(iFlow);
		Stream<Contribution<?>> stream;

		// in a regionalized result, the flow with the
		// given location can occur in processes that
		// have another location which is not the case
		// in a non-regionalized result
		if (flowIndex.isRegionalized) {
			stream = result.techIndex.content().stream().map(p -> {
				Contribution<?> c = Contribution.of(p.process);
				c.amount = result.getDirectFlowResult(p, iFlow);
				c.computeShare(total);
				return c;
			});
		} else {
			stream = processes(loc).stream().map(p -> {
				Contribution<?> c = Contribution.of(p);
				c.amount = result.getDirectFlowResult(p, iFlow);
				c.computeShare(total);
				return c;
			});
		}

		return stream.filter(c -> c.amount != 0)
				.sorted((c1, c2) -> Double.compare(c2.amount, c1.amount))
				.collect(Collectors.toList());
	}

	private List<ProcessDescriptor> processes(Location loc) {
		List<ProcessDescriptor> list = new ArrayList<>();
		result.techIndex.each((i, product) -> {
			if (!(product.process instanceof ProcessDescriptor))
				return;
			ProcessDescriptor process = (ProcessDescriptor) product.process;
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