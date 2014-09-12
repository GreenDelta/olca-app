package org.openlca.app.components;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

public class CategoryDialog extends Dialog {

	private Category category;
	private final ModelType modelType;
	private final String title;

	public CategoryDialog(Shell parentShell, String title, ModelType modelType) {
		super(parentShell);
		this.title = title;
		this.modelType = modelType;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		new Label(composite, SWT.NONE).setText(title);
		createTreeViewer(composite);
		return parent;
	}

	private void createTreeViewer(Composite composite) {
		TreeViewer viewer = NavigationTree.forSingleSelection(composite,
				modelType);
		viewer.setFilters(new ViewerFilter[] { new Filter() });
		UI.gridData(viewer.getTree(), true, true);
		viewer.addSelectionChangedListener((e) -> {
			INavigationElement<?> element = Viewers.getFirst(e.getSelection());
			if (element instanceof CategoryElement) {
				category = (Category) element.getContent();
			} else {
				category = null;
			}
		});
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 400);
	}

	public Category getSelectedCategory() {
		return category;
	}

	private class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			return element instanceof CategoryElement
					|| element instanceof ModelTypeElement;
		}
	}
}
