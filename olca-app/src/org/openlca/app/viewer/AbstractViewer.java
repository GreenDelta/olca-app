package org.openlca.app.viewer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.Viewers;

public abstract class AbstractViewer<T> implements
		org.eclipse.jface.viewers.ISelectionChangedListener {

	private List<ISelectionChangedListener<T>> listener = new ArrayList<>();
	private StructuredViewer viewer;
	private T[] input;
	private boolean hasNull;

	protected AbstractViewer(Composite parent) {
		viewer = createViewer(parent);
	}

	protected abstract StructuredViewer createViewer(Composite parent);

	protected IBaseLabelProvider getLabelProvider() {
		return new BaseLabelProvider();
	}

	protected ViewerSorter getSorter() {
		return new BaseNameSorter();
	}

	protected StructuredViewer getViewer() {
		return viewer;
	}

	public void addSelectionChangedListener(
			ISelectionChangedListener<T> listener) {
		if (this.listener.size() == 0) {
			startListening();
		}
		this.listener.add(listener);
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener<T> listener) {
		this.listener.remove(listener);
		if (this.listener.size() == 0) {
			stopListening();
		}
	}

	protected T[] getInput() {
		return this.input;
	}

	private Object[] getInternalInput() {
		return (Object[]) viewer.getInput();
	}

	protected void setInput(T[] input) {
		this.input = input;
		Object[] internalInput = new Object[input.length];
		hasNull = false;
		for (int i = 0; i < input.length; i++)
			if (input[i] == null) {
				internalInput[i] = new Null();
				hasNull = true;
			} else
				internalInput[i] = input[i];
		viewer.setInput(internalInput);
	}

	public T getSelected() {
		if (Viewers.getFirst(viewer.getSelection()) instanceof AbstractViewer.Null)
			return null;
		return Viewers.getFirst(viewer.getSelection());
	}

	public abstract String getSelectedText();

	public void select(T value) {
		internalSelect(value);
	}

	private void internalSelect(Object value) {
		if (value != null)
			viewer.setSelection(new StructuredSelection(value));
		else if (hasNull)
			viewer.setSelection(new StructuredSelection(new Null()));
		else
			viewer.setSelection(new StructuredSelection());
	}

	public void selectFirst() {
		if (getInput() == null)
			return;
		if (getInput().length == 0)
			return;
		internalSelect(getInternalInput()[0]);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		T selection = Viewers.getFirst(event.getSelection());
		for (ISelectionChangedListener<T> listener : this.listener) {
			listener.selectionChanged(selection);
		}
	}

	public void setEnabled(boolean value) {
		getViewer().getControl().setEnabled(value);
	}

	private void startListening() {
		viewer.addSelectionChangedListener(this);
	}

	private void stopListening() {
		viewer.removeSelectionChangedListener(this);
	}

	class Null {

		@Override
		public boolean equals(Object arg0) {
			return true;
		}

		@Override
		public String toString() {
			return "Null";
		}

	}

}
