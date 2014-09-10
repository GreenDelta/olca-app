package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.util.Actions;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessCostEntry;
import org.openlca.util.Strings;

class ProcessCostSection {

	private ProcessEditor editor;
	private Process process;
	private Exchange product;
	private List<ProcessCostEntry> entries;
	private TableViewer viewer;

	public ProcessCostSection(Exchange product, ProcessEditor editor) {
		this.product = product;
		this.editor = editor;
		process = editor.getModel();
		entries = loadEntries(product);
		editor.onSaved(() -> {
			process = editor.getModel();
			Exchange newProduct = findNewProduct();
			ProcessCostSection.this.product = newProduct;
			entries = loadEntries(product);
			viewer.setInput(entries);
		});
	}

	private Exchange findNewProduct() {
		for (Exchange exchange : editor.getModel().getExchanges()) {
			if (Objects.equals(exchange, product))
				return exchange;
		}
		return null;
	}

	private List<ProcessCostEntry> loadEntries(Exchange product) {
		List<ProcessCostEntry> entries = new ArrayList<>();
		for (ProcessCostEntry entry : process.getCostEntries()) {
			if (Objects.equals(entry.getExchange(), product))
				entries.add(entry);
		}
		Collections.sort(entries, new Comparator<ProcessCostEntry>() {
			@Override
			public int compare(ProcessCostEntry o1, ProcessCostEntry o2) {
				if (o1.getCostCategory() == null
						|| o2.getCostCategory() == null)
					return 0;
				return Strings.compare(o1.getCostCategory().getName(), o2
						.getCostCategory().getName());
			}
		});
		return entries;
	}

	public void render(FormToolkit toolkit, Composite parent) {
		Section section = UI.section(parent, toolkit, product.getFlow()
				.getName());
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		ProcessCostViewer costViewer = new ProcessCostViewer(editor);
		costViewer.render(toolkit, client);
		viewer = costViewer.getTableViewer();
		viewer.setInput(entries);
		bindActions(section, viewer);
	}

	private void bindActions(Section section, TableViewer viewer) {
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDeletePressed(viewer, (event) -> onRemove());
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private void onAdd() {
		Shell shell = viewer.getTable().getShell();
		ProcessCostEntryDialog dialog = new ProcessCostEntryDialog(shell,
				entries);
		int code = dialog.open();
		if (code == Window.OK) {
			ProcessCostEntry newEntry = dialog.getCostEntry();
			newEntry.setExchange(product);
			process.getCostEntries().add(newEntry);
			entries.add(newEntry);
			editor.setDirty(true);
			viewer.setInput(entries);
		}
	}

	private void onRemove() {
		List<ProcessCostEntry> selected = Viewers.getAllSelected(viewer);
		if (selected == null)
			return;
		for (ProcessCostEntry e : selected) {
			entries.remove(e);
			process.getCostEntries().remove(e);
		}
		editor.setDirty(true);
		viewer.setInput(entries);
	}
}
