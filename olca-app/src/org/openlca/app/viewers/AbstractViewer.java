package org.openlca.app.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.Viewers;

public abstract class AbstractViewer<T, V extends StructuredViewer> implements
		org.eclipse.jface.viewers.ISelectionChangedListener {

	private List<ISelectionChangedListener<T>> listener = new ArrayList<>();
	private V viewer;
	private T[] input;
	private boolean nullable;
	private String nullText;

	protected AbstractViewer(Composite parent) {
		viewer = createViewer(parent);
	}

	protected abstract V createViewer(Composite parent);

	protected IBaseLabelProvider getLabelProvider() {
		return new BaseLabelProvider();
	}

	protected ViewerSorter getSorter() {
		return new BaseNameSorter();
	}

	protected V getViewer() {
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

	public void setNullable(boolean nullable) {
		boolean changed = this.nullable != nullable;
		this.nullable = nullable;
		if (changed && getInput() != null)
			setInput(getInput());
	}

	public boolean isNullable() {
		return nullable;
	}

	protected void setNullText(String nullText) {
		this.nullText = nullText;
	}

	private Object[] getInternalInput() {
		return (Object[]) viewer.getInput();
	}

	protected void setInput(T[] input) {
		this.input = input;
		Object[] internalInput = new Object[nullable ? input.length + 1
				: input.length];
		if (nullable)
			internalInput[0] = new Null();
		for (int i = 0; i < input.length; i++)
			internalInput[nullable ? i + 1 : i] = input[i];
		viewer.setInput(internalInput);
	}

	public T getSelected() {
		if (Viewers.getFirst(viewer.getSelection()) instanceof AbstractViewer.Null)
			return null;
		return Viewers.getFirst(viewer.getSelection());
	}

	public void select(T value) {
		internalSelect(value);
	}

	private void internalSelect(Object value) {
		if (value != null)
			viewer.setSelection(new StructuredSelection(value));
		else if (nullable)
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
			return nullText == null ? "" : nullText;
		}

	}

}
