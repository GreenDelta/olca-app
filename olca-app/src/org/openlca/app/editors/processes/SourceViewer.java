package org.openlca.app.editors.processes;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.App;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.SourceDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;

class SourceViewer extends AbstractTableViewer<Source> {

	private final ProcessEditor editor;
	private final SourceDao sourceDao;

	public SourceViewer(ProcessEditor editor, Composite parent, IDatabase db) {
		super(parent);
		getViewer().getTable().setLinesVisible(false);
		getViewer().getTable().setHeaderVisible(false);
		getViewer().setLabelProvider(new LabelProvider());
		this.sourceDao = new SourceDao(db);
		this.editor = editor;
		getModifySupport().bind("", new CommentDialogModifier<Source>(
				editor.getComments(), CommentPaths::get));
		Tables.bindColumnWidths(getViewer(), 0.97);
		Tables.onDoubleClick(getViewer(), e -> {
			Source s = Viewers.getFirstSelected(getViewer());
			if (s != null) {
				App.openEditor(s);
			}
		});
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { "Name", "" };
	}

	void setInput(Process process) {
		if (process == null || process.documentation == null)
			setInput(new Source[0]);
		else {
			List<Source> sources = process.documentation.sources;
			setInput(sources);
		}
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect(ModelType.SOURCE);
		if (descriptors == null)
			return;
		boolean added = false;
		for (BaseDescriptor d : descriptors) {
			if (add(d)) {
				added = true;
			}
		}
		if (!added)
			return;
		update();
	}

	private boolean add(BaseDescriptor d) {
		if (d == null)
			return false;
		Source source = sourceDao.getForId(d.id);
		if (source == null)
			return false;
		Process p = editor.getModel();
		ProcessDocumentation doc = p.documentation;
		if (doc == null) {
			doc = new ProcessDocumentation();
			p.documentation = doc;
		}
		if (doc.sources.contains(source)) {
			return false;
		}
		return doc.sources.add(source);
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
		update();
	}

	@OnDrop
	protected void onDrop(BaseDescriptor d) {
		if (!add(d))
			return;
		update();
	}

	private void update() {
		ProcessDocumentation doc = editor.getModel().documentation;
		setInput(doc.sources);
		editor.getEventBus().post(ProcessEditor.SOURCES_CHANGED);
		editor.setDirty(true);
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
