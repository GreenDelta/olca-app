package org.openlca.app.viewers.table;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.SourceDescriptor;

public class SourceViewer extends AbstractTableViewer<Source> {

	private Process process;

	public SourceViewer(Composite parent) {
		super(parent);
	}

	public void setInput(Process process) {
		this.process = process;
		if (process == null)
			setInput(new Source[0]);
		else {
			List<Source> sources = process.getDocumentation().getSources();
			setInput(sources.toArray(new Source[sources.size()]));
		}
	}

	@OnCreate
	protected void onCreate() {
		BaseDescriptor descriptor = ObjectDialog.select(ModelType.SOURCE);
		if (descriptor != null)
			add((SourceDescriptor) descriptor);
	}

	private void add(SourceDescriptor descriptor) {
		Source source = Descriptors.toSource(descriptor);
		fireModelChanged(ModelChangeType.CREATE, source);
		setInput(process);
	}

	@OnRemove
	protected void onRemove() {
		for (Source source : getAllSelected())
			fireModelChanged(ModelChangeType.REMOVE, source);
		setInput(process);
	}

	@OnDrop
	protected void onDrop(SourceDescriptor descriptor) {
		if (descriptor != null)
			add(descriptor);
	}

}
