package org.openlca.app.editors.processes;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.BaseLabelProvider;
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
	private final ScrolledForm form;

	public SourceViewer(Composite parent, IDatabase database, ProcessEditor editor, ScrolledForm form) {
		super(parent);
		getViewer().getTable().setLinesVisible(false);
		getViewer().getTable().setHeaderVisible(false);
		getViewer().setLabelProvider(new LabelProvider());
		this.sourceDao = new SourceDao(database);
		this.editor = editor;
		this.form = form;
		getModifySupport().bind("", new CommentDialogModifier<Source>(editor.getComments(), CommentPaths::get));
		Tables.bindColumnWidths(getViewer(), 0.97);
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

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { "Name", "" };
	}

	public void setInput(Process process) {
		if (process == null || process.documentation == null)
			setInput(new Source[0]);
		else {
			List<Source> sources = process.documentation.sources;
			setInput(sources);
		}
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ModelSelectionDialog.multiSelect(ModelType.SOURCE);
		if (descriptors == null)
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof SourceDescriptor))
				continue;
			add((SourceDescriptor) descriptor);
		}
		ProcessDocumentation doc = editor.getModel().documentation;
		setInput(doc.sources);
		form.reflow(true);
		editor.setDirty(true);
	}

	private void add(SourceDescriptor descriptor) {
		Source source = sourceDao.getForId(descriptor.id);
		Process process = editor.getModel();
		ProcessDocumentation doc = process.documentation;
		if (doc == null) {
			doc = new ProcessDocumentation();
			process.documentation = doc;
		}
		if (doc.sources.contains(source))
			return;
		doc.sources.add(source);
	}

	@OnRemove
	protected void onRemove() {
		Process process = editor.getModel();
		if (process == null || process.documentation == null)
			return;
		ProcessDocumentation doc = process.documentation;
		for (Source source : getAllSelected()) {
			doc.sources.remove(source);
		}
		setInput(doc.sources);
		editor.setDirty(true);
		form.reflow(true);
	}

	@OnDrop
	protected void onDrop(SourceDescriptor descriptor) {
		if (descriptor != null)
			add(descriptor);
	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column == 0)
				return getImage(element);
			return Images.get(editor.getComments(), CommentPaths.get((Source) element));
		}

		@Override
		public String getColumnText(Object element, int column) {
			if (column == 0)
				return getText(element);
			return null;
		}

	}

}
