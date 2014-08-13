package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;

public abstract class EditorAction extends Action implements UpdateAction {

	private ProductSystemGraphEditor editor;

	protected EditorAction() {

	}

	protected EditorAction(String text, int style) {
		super(text, style);
	}

	@Override
	public void update() {
		setEnabled(accept(editor.getSelection()));
	}

	public void setEditor(ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

	protected ProductSystemGraphEditor getEditor() {
		return editor;
	}

	protected abstract boolean accept(ISelection selection);

	@SuppressWarnings("unchecked")
	protected <T> T getSingleSelectionOfType(ISelection selection, Class<T> type) {
		if (selection == null)
			return null;
		if (selection.isEmpty())
			return null;
		if (!(selection instanceof IStructuredSelection))
			return null;

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() > 1)
			return null;
		if (!(sel.getFirstElement() instanceof EditPart))
			return null;
		Object model = ((EditPart) sel.getFirstElement()).getModel();
		if (model == null || model.getClass() != type)
			return null;
		return (T) model;
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> getMultiSelectionOfType(ISelection selection, Class<T> type) {
		List<T> models = new ArrayList<>();
		if (selection == null)
			return Collections.emptyList();
		if (selection.isEmpty())
			return Collections.emptyList();
		if (!(selection instanceof IStructuredSelection))
			return Collections.emptyList();

		IStructuredSelection sel = (IStructuredSelection) selection;
		for (Object o : sel.toArray())
			if (o instanceof EditPart) {
				Object model = ((EditPart) o).getModel();
				if (model != null && model.getClass() == type) {
					T elem = (T) model;
					models.add(elem);
				}
			}
		return models;
	}

}
