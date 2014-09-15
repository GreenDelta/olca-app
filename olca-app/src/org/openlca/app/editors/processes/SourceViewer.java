package org.openlca.app.editors.processes;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.App;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.util.Tables;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.SourceDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.SourceDescriptor;

class SourceViewer extends AbstractTableViewer<Source> {

	private final ProcessEditor editor;
	private final SourceDao sourceDao;

	public SourceViewer(Composite parent, IDatabase database,
			ProcessEditor editor) {
		super(parent);
		this.sourceDao = new SourceDao(database);
		this.editor = editor;
		addDoubleClickHandler();
	}

	private void addDoubleClickHandler() {
		Tables.onDoubleClick(getViewer(), (event) -> {
			TableItem item = Tables.getItem(getViewer(), event);
			if (item == null) {
				onCreate();
				return;
			}
			Source source = getSelected();
			if (source != null)
				App.openEditor(source);
		});
	}

	public void setInput(Process process) {
		if (process == null || process.getDocumentation() == null)
			setInput(new Source[0]);
		else {
			List<Source> sources = process.getDocumentation().getSources();
			setInput(sources);
		}
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect(ModelType.SOURCE);
		if (descriptors == null)
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof SourceDescriptor))
				continue;
			add((SourceDescriptor) descriptor);
		}
	}

	private void add(SourceDescriptor descriptor) {
		Source source = sourceDao.getForId(descriptor.getId());
		Process process = editor.getModel();
		ProcessDocumentation doc = process.getDocumentation();
		if (doc == null) {
			doc = new ProcessDocumentation();
			process.setDocumentation(doc);
		}
		doc.getSources().add(source);
		setInput(doc.getSources());
		editor.setDirty(true);
	}

	@OnRemove
	protected void onRemove() {
		Process process = editor.getModel();
		if (process == null || process.getDocumentation() == null)
			return;
		ProcessDocumentation doc = process.getDocumentation();
		for (Source source : getAllSelected()) {
			doc.getSources().remove(source);
		}
		setInput(doc.getSources());
		editor.setDirty(true);
	}

	@OnDrop
	protected void onDrop(SourceDescriptor descriptor) {
		if (descriptor != null)
			add(descriptor);
	}

}
