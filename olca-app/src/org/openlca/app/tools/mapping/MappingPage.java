package org.openlca.app.tools.mapping;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;

class MappingPage extends FormPage {

	private final MappingTool tool;
	TableViewer table;

	public MappingPage(MappingTool tool) {
		super(tool, "MappingPage", "Flow mapping");
		this.tool = tool;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "Flow mapping");
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createInfoSection(tk, body);
		createTable(body, tk);
		form.reflow(true);
	}

	private void createInfoSection(FormToolkit tk, Composite body) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		var name = UI.formText(comp, tk, M.Name);
		Controls.set(name, this.tool.mapping.name);

		UI.formLabel(comp, tk, "Source system");
		ProviderRow sourceRow = new ProviderRow(comp, tk);
		sourceRow.onSelect = p -> tool.sourceSystem = p;

		UI.formLabel(comp, tk, "Target system");
		ProviderRow targetRow = new ProviderRow(comp, tk);
		targetRow.onSelect = p -> tool.targetSystem = p;

		UI.filler(comp);
		Button checkButton = tk.createButton(comp, "Check mappings", SWT.NONE);
		Runnable updateCheckState = () -> {
			if (tool.checked.get()) {
				checkButton.setImage(Icon.ACCEPT.get());
				checkButton.setToolTipText("Click to check the mappings.");
			} else {
				checkButton.setImage(Icon.WARNING.get());
				checkButton.setToolTipText("No check was performed yet.");
			}
		};
		updateCheckState.run();
		Controls.onSelect(checkButton, _e -> {
			if (tool.sourceSystem == null || tool.targetSystem == null) {
				MsgBox.warning("No source or target system",
						"You need to select a source and target"
								+ " system against which you want"
								+ " to check the mapping.");
				return;
			}
			App.runWithProgress("Check mappings", this::syncMappings, () -> {
				tool.checked.set(true);
				table.setInput(tool.mapping.entries);
				updateCheckState.run();
				tool.setDirty();
			});
		});
	}

	private void createTable(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, "Flow mapping");
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		table = Tables.createViewer(
				comp,
				"Status",
				"Source flow",
				"Source category",
				"Target flow",
				"Target category",
				"Conversion factor",
				"Default provider");
		TableLabel label = new TableLabel();
		table.setLabelProvider(new TableLabel());
		Viewers.sortByLabels(table, label);
		double w = 1.0 / 7.0;
		Tables.bindColumnWidths(table, w, w, w, w, w, w, w);
		table.setInput(this.tool.mapping.entries);
		bindActions(section, table);
	}

	private void bindActions(Section section, TableViewer table) {
		Action add = Actions.onAdd(() -> {
			var e = new FlowMapEntry(new FlowRef(), new FlowRef(), 1);
			if (Dialog.OK != MappingDialog.open(tool, e))
				return;
			tool.mapping.entries.add(e);
			table.refresh();
			tool.setDirty();
		});

		Action edit = Actions.onEdit(() -> {
			FlowMapEntry e = Viewers.getFirstSelected(table);
			if (e == null)
				return;
			if (Dialog.OK == MappingDialog.open(tool, e)) {
				table.refresh();
				tool.setDirty();
			}
		});
		Tables.onDoubleClick(table, _e -> edit.run());

		Action delete = Actions.onRemove(() -> {
			List<FlowMapEntry> entries = Viewers.getAllSelected(table);
			if (entries.isEmpty())
				return;
			tool.mapping.entries.removeAll(entries);
			table.refresh();
			tool.setDirty();
		});
		Tables.onDeletePressed(table, $ -> delete.run());

		Action copy = TableClipboard.onCopySelected(table);

		Actions.bind(section, add, edit, delete);
		Actions.bind(table, add, edit, copy, delete);
	}

	private void syncMappings() {
		// run the sync functions in two separate threads and wait for
		// them to finish
		Runnable st = null;
		Runnable tt = null;
		if (tool.sourceSystem != null) {
			Stream<FlowRef> stream = tool.mapping.entries
					.stream().map(FlowMapEntry::sourceFlow);
			st = () -> tool.sourceSystem.sync(stream);
		}
		if (tool.targetSystem != null) {
			Stream<FlowRef> stream = tool.mapping.entries
					.stream().map(FlowMapEntry::targetFlow);
			tt = () -> tool.targetSystem.sync(stream);
		}
		if (st == null && tt == null)
			return;
		try {
			var exec = Executors.newFixedThreadPool(2);
			if (st != null) {
				exec.execute(st);
			}
			if (tt != null) {
				exec.execute(tt);
			}
			exec.shutdown();
			exec.awaitTermination(1, TimeUnit.DAYS);
		} catch (Exception e) {
			ErrorReporter.on("Failed to sync flow mappings", e);
		}
	}
}
