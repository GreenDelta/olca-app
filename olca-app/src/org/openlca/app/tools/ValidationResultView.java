package org.openlca.app.tools;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.validation.Item;

public class ValidationResultView extends SimpleFormEditor {

	private List<Item> items;

	public static void open(List<Item> items) {
		if (items == null)
			return;
		var cacheKey = Cache.getAppCache().put(items);
		Editors.open(
			new SimpleEditorInput(cacheKey, "Validation result"),
			"ValidationResultView");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		var inp = (SimpleEditorInput) input;
		items = Cache.getAppCache().remove(inp.id);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final List<Item> items;

		Page(ValidationResultView view) {
			super(view, "ValidationResultView", "Validation results");
			this.items = view.items;
		}

		@Override
		public void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "Validation results");
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			var table = Tables.createViewer(body, M.DataSet, M.Message);
			Tables.bindColumnWidths(table, 0.2, 0.8);
			var label = new Label();
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1);
			table.setInput(items);
			bindActions(table);
		}

		private void bindActions(TableViewer table) {
			var onOpen = Actions.onOpen(() -> {
				var e = Viewers.getFirstSelected(table);
				if (!(e instanceof Item item))
					return;
				if (item.model() instanceof RootDescriptor d) {
					if (d.type != null
						&& d.type != ModelType.CATEGORY
						&& d.type.isRoot()) {
						App.open((RootDescriptor) item.model());
					}
				}
			});
			var onCopy = TableClipboard.onCopySelected(table);
			Actions.bind(table, onOpen, onCopy);
			Tables.onDoubleClick(table, $ -> onOpen.run());
		}
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			if (col == 0 && item.hasModel())
				return Images.get(item.model());
			if (col == 1) {
				if (item.isError())
					return Icon.ERROR.get();
				if (item.isWarning())
					return Icon.WARNING.get();
				if (item.isOk())
					return Icon.ACCEPT.get();
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			if (col == 0)
				return item.hasModel()
					? Labels.name(item.model())
					: null;
			return col == 1
				? item.message()
				: null;
		}
	}

}
