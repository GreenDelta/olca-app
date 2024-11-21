package org.openlca.app.components;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

public class CategoryDialog extends Dialog {

	private Category category;
	private final ModelType modelType;
	private final String title;

	private CategoryDialog(String title, ModelType modelType) {
		super(UI.shell());
		this.title = title;
		this.modelType = modelType;
	}

	public static State selectFor(ModelType type) {
		if (type == null)
			return State.cancelled();
		var dialog = new CategoryDialog("Select category", type);
		return dialog.open() == OK
				? State.selected(dialog.category)
				: State.cancelled();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = UI.composite(parent);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		new Label(composite, SWT.NONE).setText(title);
		createTreeViewer(composite);
		return parent;
	}

	private void createTreeViewer(Composite composite) {
		var viewer = NavigationTree.forSingleSelection(composite, modelType);
		viewer.setFilters(new Filter());
		UI.gridData(viewer.getTree(), true, true);
		viewer.addSelectionChangedListener(e -> {
			if (Selections.firstOf(e) instanceof CategoryElement elem) {
				category = elem.getContent();
			} else {
				category = null;
			}
		});
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 400);
	}

	private static class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			return element instanceof CategoryElement
					|| element instanceof ModelTypeElement;
		}
	}

	public record State(Category category, boolean isCancelled) {

		private static State cancelled() {
			return new State(null, true);
		}

		private static State selected(Category category) {
			return new State(category, false);
		}

		public boolean isEmpty() {
			return category == null;
		}
	}
}
