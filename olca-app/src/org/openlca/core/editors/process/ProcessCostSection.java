package org.openlca.core.editors.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductCostEntryDao;
import org.openlca.core.editors.IEditorComponent;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProductCostEntry;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.Viewers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessCostSection implements IEditorComponent {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProcessEditor editor;
	private Exchange product;
	private IDatabase database;
	private List<ProductCostEntry> entries;
	private List<ProductCostEntry> removals = new ArrayList<>();
	private TableViewer viewer;

	public ProcessCostSection(Exchange product, IDatabase database,
			ProcessEditor editor) {
		this.product = product;
		this.database = database;
		this.entries = loadEntries(product, database);
		this.editor = editor;
		editor.registerComponent(this);
	}

	private List<ProductCostEntry> loadEntries(Exchange product,
			IDatabase database) {
		try {
			ProductCostEntryDao dao = new ProductCostEntryDao(
					database);
			return dao.getAllForProduct(product.getId());
		} catch (Exception e) {
			log.error("Could not load cost entries", e);
			return Collections.emptyList();
		}
	}

	public void render(FormToolkit toolkit, Composite parent) {
		Section section = UI.section(parent, toolkit, product.getFlow()
				.getName());
		UI.bindActions(section, new AddAction(), new RemoveAction());
		Composite client = UI.sectionClient(section, toolkit);
		ProcessCostViewer costViewer = new ProcessCostViewer(editor);
		costViewer.render(toolkit, client);
		viewer = costViewer.getTableViewer();
		UI.gridData(viewer.getTable(), true, true);
		viewer.setInput(entries);
	}

	@Override
	public void onSaved() {
		ProductCostEntryDao dao = new ProductCostEntryDao(
				database);
		try {
			for (ProductCostEntry entry : entries) {
				if (!dao.contains(entry.getId()))
					dao.insert(entry);
				else
					dao.update(entry);
			}
			for (ProductCostEntry removal : removals)
				if (dao.contains(removal.getId()))
					dao.delete(removal);
			removals.clear();
		} catch (Exception e) {
			log.error("Failed to save cost changes", e);
		}
	}

	@Override
	public void onChange() {
		// TODO: dispose this section and delete all entries when the product
		// was removed
	}

	private class AddAction extends Action {

		public AddAction() {
			setToolTipText("Add");
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
		}

		@Override
		public void run() {
			Shell shell = viewer.getTable().getShell();
			ProcessCostEntryDialog dialog = new ProcessCostEntryDialog(shell,
					database, entries);
			int code = dialog.open();
			if (code == Window.OK) {
				ProductCostEntry newEntry = dialog.getCostEntry();
				newEntry.setExchangeId(product.getId());
				newEntry.setProcessId(editor.getModelComponent().getId());
				entries.add(newEntry);
				editor.fireChange();
				viewer.setInput(entries);
			}
		}
	}

	private class RemoveAction extends Action {

		public RemoveAction() {
			setToolTipText("Remove");
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		}

		@Override
		public void run() {
			ProductCostEntry e = Viewers.getFirstSelected(viewer);
			if (e != null) {
				entries.remove(e);
				removals.add(e);
				editor.fireChange();
				viewer.setInput(entries);
			}
		}
	}

}
