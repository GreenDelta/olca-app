package org.openlca.app.results.impacts;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.openlca.app.results.ContributionCutoff;

class TreeContent extends ArrayContentProvider
		implements ITreeContentProvider, ContributionCutoff.CutoffContentProvider {

	private double cutoff;
	private final ImpactTreePage page;

	TreeContent(ImpactTreePage page) {
		this.page = page;
	}

	@Override
	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	@Override
	public Object[] getChildren(Object obj) {
		if (!(obj instanceof TreeItem item))
			return null;
		if (item.isLeaf())
			return null;
		if (item.isRoot()) {
			var childs = page.flowsFirst
					? item.enviNodesOf(page.items.enviFlows(), cutoff)
					: item.techNodesOf(page.items.techFlows(), cutoff);
			return childs.toArray();
		}
		var childs = item.isTechItem()
				? item.enviLeafsOf(page.items.enviFlows(), cutoff)
				: item.techLeafsOf(page.items.techFlows(), cutoff);
		return childs.toArray();
	}

	@Override
	public Object getParent(Object o) {
		return o instanceof TreeItem item
				? item.parent()
				: null;
	}

	@Override
	public boolean hasChildren(Object o) {
		return o instanceof TreeItem item && !item.isLeaf();
	}
}
