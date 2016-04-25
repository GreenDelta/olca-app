package org.openlca.app.components.replace;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.openlca.core.model.descriptors.FlowDescriptor;

class FilterOnKey extends KeyAdapter {

	private boolean active = true;
	private final ComboViewer viewer;
	private final NameFilter filter;
	private final Supplier<List<?>> inputSupplier;

	FilterOnKey(ComboViewer viewer, NameFilter filter, Supplier<List<?>> inputSupplier) {
		this.viewer = viewer;
		this.filter = filter;
		this.inputSupplier = inputSupplier;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!active)
			return;
		active = false;
		String text = viewer.getCCombo().getText();
		Point caret = viewer.getCCombo().getSelection();
		filter.filter = text.toLowerCase();
		viewer.setInput(inputSupplier.get());
		viewer.setSelection(new StructuredSelection(new FlowDescriptor()));
		viewer.getCCombo().setText(text);
		viewer.getCCombo().setSelection(caret);
		active = true;
	}

}
