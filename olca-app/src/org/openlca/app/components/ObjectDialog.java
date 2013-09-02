package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class ObjectDialog extends FormDialog {

	private final List<ViewerFilter> filters = new ArrayList<>();
	private final ModelType modelType;
	private boolean multiSelection = false;

	private TreeViewer viewer;
	private Text searchText;
	private BaseDescriptor[] selection;

	public static BaseDescriptor select(ModelType type, ViewerFilter... filters) {
		ObjectDialog dialog = new ObjectDialog(UI.shell(), type);
		if (filters != null)
			for (ViewerFilter filter : filters)
				dialog.filters.add(filter);
		if (dialog.open() == OK) {
			BaseDescriptor[] selection = dialog.getSelection();
			if (selection == null || selection.length == 0)
				return null;
			return selection[0];
		}
		return null;
	}

	public static BaseDescriptor[] multiSelect(ModelType type,
			ViewerFilter... filters) {
		ObjectDialog dialog = new ObjectDialog(UI.shell(), type);
		dialog.multiSelection = true;
		if (filters != null)
			for (ViewerFilter filter : filters)
				dialog.filters.add(filter);
		if (dialog.open() == OK)
			return dialog.getSelection();
		return null;
	}

	private ObjectDialog(Shell parentShell, ModelType modelType) {
		super(parentShell);
		this.modelType = modelType;
		setShellStyle(SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL
				| SWT.RESIZE);
		setBlockOnOpen(true);
		filters.add(new NameFilter());
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		Composite composite = UI.formBody(form.getForm(), form.getToolkit());
		UI.gridLayout(composite, 1);
		String title = "Select model" + (multiSelection ? "(s)" : "");
		UI.formHeader(form, title);

		UI.applyBoldFont(UI.formLabel(composite, form.getToolkit(),
				"Filter by name"));

		searchText = UI.formText(composite, SWT.SEARCH);
		searchText.addModifyListener(new SearchTextModifyListener());

		Composite actionComposite = form.getToolkit()
				.createComposite(composite);
		UI.gridLayout(actionComposite, 2, 2, 0);
		UI.gridData(actionComposite, false, false);

		Button expandButton = form.getToolkit().createButton(actionComposite,
				"", SWT.PUSH);
		expandButton.setImage(ImageType.EXPAND_ICON.get());
		expandButton.addSelectionListener(new ExpandSelectionListener());
		Button collapseButton = form.getToolkit().createButton(actionComposite,
				"", SWT.PUSH);
		collapseButton.addSelectionListener(new CollapseSelectionListener());
		collapseButton.setImage(ImageType.COLLAPSE_ICON.get());

		if (multiSelection)
			viewer = NavigationTree.forMultiSelection(composite, modelType);
		else
			viewer = NavigationTree.forSingleSelection(composite, modelType);
		viewer.setFilters(filters.toArray(new ViewerFilter[filters.size()]));

		viewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer.addSelectionChangedListener(new SelectionChangedListener());
		viewer.addDoubleClickListener(new DoubleClickListener());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 600);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point loc = super.getInitialLocation(initialSize);
		int marginTop = (getParentShell().getSize().y - initialSize.y) / 3;
		if (marginTop < 0)
			marginTop = 0;
		return new Point(loc.x, loc.y + marginTop);
	}

	private BaseDescriptor[] getSelection() {
		return selection;
	}

	private boolean matches(ModelElement element) {
		return element.getContent().getName().toLowerCase()
				.contains(searchText.getText().toLowerCase());
	}

	private class SearchTextModifyListener implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			viewer.refresh();
		}

	}

	private class ExpandSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			viewer.expandAll();
		}
	}

	private class CollapseSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			viewer.collapseAll();
		}
	}

	private class SelectionChangedListener implements ISelectionChangedListener {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection() != null && !event.getSelection().isEmpty()) {
				IStructuredSelection s = (IStructuredSelection) event
						.getSelection();

				List<BaseDescriptor> currentSelection = new ArrayList<>();
				for (Object selected : s.toArray())
					currentSelection
							.addAll(getSelection((INavigationElement<?>) selected));
				selection = currentSelection
						.toArray(new BaseDescriptor[currentSelection.size()]);
			}
			getButton(IDialogConstants.OK_ID).setEnabled(
					selection != null && selection.length > 0);
		}
	}

	private List<BaseDescriptor> getSelection(INavigationElement<?> element) {
		List<BaseDescriptor> currentSelection = new ArrayList<>();
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			if (matches(modelElement))
				currentSelection.add(modelElement.getContent());
		} else
			for (INavigationElement<?> child : element.getChildren())
				currentSelection.addAll(getSelection(child));
		return currentSelection;
	}

	private class DoubleClickListener implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection currentSelection = (IStructuredSelection) event
					.getSelection();
			if (currentSelection.getFirstElement() instanceof ModelElement) {
				ModelElement element = (ModelElement) currentSelection
						.getFirstElement();
				BaseDescriptor modelComponent = element.getContent();
				selection = new BaseDescriptor[] { modelComponent };
				okPressed();
			}
		}

	}

	private class NameFilter extends ViewerFilter {

		private boolean select(INavigationElement<?> element) {
			if (element instanceof ModelElement)
				return matches((ModelElement) element);

			for (INavigationElement<?> child : element.getChildren())
				if (select(child))
					return true;
			return false;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if ("".equals(searchText.getText()))
				return true;
			else
				return select((INavigationElement<?>) element);
		}
	}

}
