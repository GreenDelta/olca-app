package org.openlca.app.tools.mapping;

import java.util.Optional;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.replacer.Replacer;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.io.maps.FlowMapEntry;

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
		Composite comp = UI.formSection(body, tk, M.GeneralInformation);
		Text name = UI.formText(comp, tk, M.Name);
		Controls.set(name, this.tool.mapping.name);

		UI.formLabel(comp, tk, "Source system");
		ProviderRow sourceRow = new ProviderRow(comp, tk);

		UI.formLabel(comp, tk, "Target system");
		ProviderRow targetRow = new ProviderRow(comp, tk);

		UI.filler(comp);
		Button applyButton = tk.createButton(comp, "Apply", SWT.NONE);
		applyButton.setImage(Icon.ACCEPT.get());
		Controls.onSelect(applyButton, e -> {
			Optional<ReplacerConfig> opt = ReplacerDialog.open(
					tool.mapping, tool.sourceSystem);
			if (!opt.isPresent())
				return;
			Replacer replacer = new Replacer(opt.get());
			App.runWithProgress("Replace flows ...", replacer, () -> {
				table.setInput(tool.mapping.entries);
			});
		});
		applyButton.setEnabled(false);
		Runnable checkApply = () -> {
			if ((tool.sourceSystem instanceof DBProvider)
					&& tool.targetSystem != null) {
				applyButton.setEnabled(true);
			} else {
				applyButton.setEnabled(false);
			}
		};

		// event handlers for the source system
		sourceRow.onSelect = p -> {
			tool.sourceSystem = p;
			checkApply.run();
		};
		sourceRow.onSync = () -> {
			if (tool.sourceSystem == null)
				return;
			App.runWithProgress(
					"Synchronize source flows",
					() -> tool.targetSystem.sync(
							tool.mapping.entries.stream().map(e -> e.sourceFlow)),
					() -> table.setInput(tool.mapping.entries));
		};

		// event handlers for the target system
		targetRow.onSelect = p -> {
			tool.targetSystem = p;
			checkApply.run();
		};
		targetRow.onSync = () -> {
			if (tool.targetSystem == null)
				return;
			App.runWithProgress(
					"Synchronize target flows",
					() -> tool.targetSystem.sync(
							tool.mapping.entries.stream().map(e -> e.targetFlow)),
					() -> table.setInput(tool.mapping.entries));
		};
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
				"Source unit",
				"Target flow",
				"Target category",
				"Target unit",
				"Conversion factor",
				"Default provider");
		table.setLabelProvider(new TableLabel());
		double w = 1.0 / 9.0;
		Tables.bindColumnWidths(table, w, w, w, w, w, w, w, w, w);
		table.setInput(this.tool.mapping.entries);
		bindActions(section, table);
	}

	private void bindActions(Section section, TableViewer table) {
		Action add = Actions.onAdd(() -> {
			FlowMapEntry e = new FlowMapEntry();
			if (Dialog.OK != MappingDialog.open(tool, e))
				return;
			tool.mapping.entries.add(e);
			table.refresh();
		});

		Action edit = Actions.onEdit(() -> {
			FlowMapEntry e = Viewers.getFirstSelected(table);
			if (e == null)
				return;
			if (Dialog.OK == MappingDialog.open(tool, e)) {
				table.refresh();
			}
		});
		Tables.onDoubleClick(table, _e -> edit.run());

		Action delete = Actions.onRemove(() -> {
			FlowMapEntry e = Viewers.getFirstSelected(table);
			if (e == null)
				return;
			tool.mapping.entries.remove(e);
			table.refresh();
		});

		Actions.bind(section, add, edit, delete);
		Actions.bind(table, add, edit, delete);
	}
}