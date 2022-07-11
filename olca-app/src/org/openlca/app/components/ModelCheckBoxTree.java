package org.openlca.app.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.navigation.NavigationComparator;
import org.openlca.app.navigation.NavigationContentProvider;
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.filters.ModelTypeFilter;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

public class ModelCheckBoxTree implements ICheckStateListener {

	private final ModelType[] types;
	private CheckboxTreeViewer tree;
	private Runnable onChange;

	public ModelCheckBoxTree(ModelType... types) {
		this.types = types;
	}

	public void drawOn(Composite comp) {
		drawOn(comp, null);
	}

	public void onSelectionChanged(Runnable fn) {
		this.onChange = fn;
	}

	public void drawOn(Composite comp, FormToolkit tk) {
		tree = new CheckboxTreeViewer(comp,
			SWT.VIRTUAL | SWT.MULTI | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(new NavigationContentProvider());
		tree.setLabelProvider(NavigationLabelProvider.withoutRepositoryState());
		tree.setComparator(new NavigationComparator());
		tree.addFilter(new ModelTypeFilter(types));
		tree.addCheckStateListener(this);
		ColumnViewerToolTipSupport.enableFor(tree);
		if (tk != null) {
			tk.adapt(tree.getTree());
		}

		// compute a height hint
		GridData data = UI.gridData(tree.getTree(), true, true);
		data.minimumHeight = 120;
		Point p = comp.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		data.heightHint = Math.max(p.y, 120);

		if (types == null || types.length == 0)
			return;
		if (types.length == 1) {
			tree.setInput(Navigator.findElement(types[0]));
		} else {
			List<INavigationElement<?>> elems = Arrays.stream(types)
				.map(Navigator::findElement)
				.filter(Objects::nonNull)
				.filter(elem -> !elem.getChildren().isEmpty())
				.collect(Collectors.toList());
			tree.setInput(elems);
		}
		tree.expandToLevel(2);
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent evt) {
		tree.getControl().setRedraw(false);
		INavigationElement<?> elem = (INavigationElement<?>) evt.getElement();
		tree.setGrayed(elem, false);
		checkChildren(elem, evt.getChecked());
		checkParent(elem);
		tree.getControl().setRedraw(true);
		if (onChange != null) {
			onChange.run();
		}
	}

	private void checkChildren(INavigationElement<?> elem, boolean state) {
		for (INavigationElement<?> child : elem.getChildren()) {
			tree.setGrayed(child, false);
			tree.setChecked(child, state);
			if (!(child instanceof ModelElement)) {
				checkChildren(child, state);
			}
		}
	}

	private void checkParent(INavigationElement<?> elem) {
		INavigationElement<?> parent = elem.getParent();
		if (parent == null)
			return;
		boolean checked = false;
		boolean all = true;
		for (INavigationElement<?> child : parent.getChildren()) {
			checked = tree.getChecked(child) || tree.getGrayed(child);
			if (!tree.getChecked(child) || tree.getGrayed(child))
				all = false;
		}
		tree.setGrayed(parent, !all && checked);
		tree.setChecked(parent, checked);
		checkParent(parent);
	}

	public List<RootDescriptor> getSelection() {
		Object[] elems = tree.getCheckedElements();
		if (elems == null || elems.length == 0)
			return Collections.emptyList();
		return Arrays.stream(elems)
			.filter(e -> e instanceof ModelElement)
			.map(e -> ((ModelElement) e).getContent())
			.collect(Collectors.toList());
	}

}
