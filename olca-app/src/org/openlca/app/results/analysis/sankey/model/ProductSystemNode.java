package org.openlca.app.results.analysis.sankey.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.core.model.ProductSystem;

public class ProductSystemNode extends Node implements PropertyChangeListener {

	private double cutoff = 0;
	private SankeyDiagram editor;
	private Object selection;
	private List<Long> processIds = new ArrayList<>();
	private ProductSystem productSystem;

	public ProductSystemNode(ProductSystem productSystem, SankeyDiagram editor,
			Object selection, double cutoff) {
		this.productSystem = productSystem;
		this.editor = editor;
		this.selection = selection;
		this.cutoff = cutoff;
	}

	@Override
	public boolean addChild(Node child) {
		boolean added = super.addChild(child);
		if (added && child instanceof ProcessNode) {
			processIds.add(((ProcessNode) child).getProcess().getId());
		}
		return added;
	}

	public boolean containsProcess(long id) {
		return processIds.contains(id);
	}

	@Override
	public void dispose() {
		for (Node node : getChildrenArray()) {
			node.dispose();
		}
		getChildrenArray().clear();
		editor = null;
	}

	public double getCutoff() {
		return cutoff;
	}

	public SankeyDiagram getEditor() {
		return editor;
	}

	public Object getSelection() {
		return selection;
	}

	public ProductSystem getProductSystem() {
		return productSystem;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		listeners.firePropertyChange(evt);
	}

}
