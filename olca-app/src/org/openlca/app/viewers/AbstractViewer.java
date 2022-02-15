package org.openlca.app.viewers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class AbstractViewer<T, V extends StructuredViewer> implements
	org.eclipse.jface.viewers.ISelectionChangedListener {

	private final List<Consumer<T>> listener = new ArrayList<>();
	private final V viewer;
	private boolean nullable;
	private String nullText;
	protected Object[] viewerParameters;

	protected AbstractViewer(Composite parent, Object... viewerParameters) {
		this.viewerParameters = viewerParameters;
		viewer = createViewer(parent);
	}

	protected abstract V createViewer(Composite parent);

	protected IBaseLabelProvider getLabelProvider() {
		return new BaseLabelProvider();
	}

	public V getViewer() {
		return viewer;
	}

	public Control getControl() {
		return viewer.getControl();
	}

	public void refresh() {
		viewer.refresh();
	}

	public void addSelectionChangedListener(
		Consumer<T> listener) {
		if (this.listener.size() == 0) {
			startListening();
		}
		this.listener.add(listener);
	}

	public void removeSelectionChangedListener(
		Consumer<T> listener) {
		this.listener.remove(listener);
		if (this.listener.size() == 0) {
			stopListening();
		}
	}

	@SuppressWarnings("unchecked")
	public Consumer<T>[] getSelectionChangedListeners() {
		return listener.toArray(new Consumer[0]);
	}

	/**
	 * Adds an empty/null value as first element in the viewer, you can specify the
	 * text of this null element via #setNullText
	 */
	public void setNullable(boolean nullable) {
		boolean changed = this.nullable != nullable;
		this.nullable = nullable;
		if (changed && viewer.getInput() != null)
			setInternalInput((Object[]) viewer.getInput());
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

	public void setInput(Collection<T> collection) {
		if (collection == null)
			setInternalInput(new Object[0]);
		else
			setInternalInput(collection.toArray());
	}

	public void setInput(T[] input) {
		if (input == null)
			setInternalInput(new Object[0]);
		else
			setInternalInput(input);
	}

	private void setInternalInput(Object[] input) {
		Object[] internalInput = new Object[nullable ? input.length + 1
			: input.length];
		if (nullable)
			internalInput[0] = new Null();
		for (int i = 0; i < input.length; i++)
			internalInput[nullable ? i + 1 : i] = input[i];
		viewer.setInput(internalInput);
	}

	@SuppressWarnings("unchecked")
	public T getSelected() {
		var first = Viewers.getFirstSelected(viewer);
		return first instanceof AbstractViewer.Null
			? null
			: (T) first;
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
		Object[] input = getInternalInput();
		if (input == null)
			return;
		if (input.length == 0)
			return;
		internalSelect(input[0]);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void selectionChanged(SelectionChangedEvent event) {
		Object obj = Selections.firstOf(event);
		T selection = null;
		if (!Null.class.isInstance(obj))
			selection = (T) obj;
		for (Consumer<T> listener : this.listener) {
			listener.accept(selection);
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

	protected class Null {

		@Override
		public boolean equals(Object other) {
			return other == null
				|| this.getClass().equals(other.getClass());
		}

		@Override
		public String toString() {
			return nullText == null ? "" : nullText;
		}

	}

}
