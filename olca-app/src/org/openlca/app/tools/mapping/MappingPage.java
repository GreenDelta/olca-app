package org.openlca.app.tools.mapping;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.ILCDProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.model.JsonProvider;
import org.openlca.app.tools.mapping.model.ProviderType;
import org.openlca.app.tools.mapping.replacer.Replacer;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.IDatabase;
import org.openlca.io.maps.FlowMapEntry;

class MappingPage extends FormPage {

	private final MappingTool tool;
	private TableViewer table;

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
					() -> tool.sourceSystem.syncSourceFlows(tool.mapping),
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
					() -> tool.targetSystem.syncTargetFlows(tool.mapping),
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

		// TODO: bind actions: edit/delete/sync(all)
		Tables.onDoubleClick(table, _e -> {
			FlowMapEntry e = Viewers.getFirstSelected(table);
			if (e != null) {
				if (Dialog.OK == MappingDialog.open(tool, e)) {
					table.refresh();
				}
			}
		});
	}

	private class ProviderRow {

		Consumer<IProvider> onSelect;
		Runnable onSync;

		ProviderRow(Composite parent, FormToolkit tk) {

			Composite inner = tk.createComposite(parent);
			UI.gridLayout(inner, 4, 5, 0);
			ImageHyperlink dbLink = tk.createImageHyperlink(inner, SWT.NONE);
			dbLink.setImage(Icon.DATABASE.get());
			dbLink.setToolTipText("Select database");
			ImageHyperlink fileLink = tk.createImageHyperlink(inner, SWT.NONE);
			fileLink.setImage(Icon.FILE.get());
			fileLink.setToolTipText("Select file");
			Label label = UI.formLabel(inner, "- none -");
			ImageHyperlink syncLink = tk.createImageHyperlink(inner, SWT.NONE);
			syncLink.setImage(Icon.REFRESH.get());
			syncLink.setToolTipText("Synchronize flows ...");
			syncLink.setVisible(false);
			Controls.onClick(syncLink, e -> {
				if (onSync != null)
					onSync.run();
			});

			// select database as provider
			Controls.onClick(dbLink, e -> {
				IDatabase db = Database.get();
				if (db == null) {
					Error.showBox(M.NoDatabaseOpened);
					return;
				}
				DBProvider provider = new DBProvider(db);
				fireSelect(label, provider);
				syncLink.setVisible(true);
			});

			// select a file as provider
			Controls.onClick(fileLink, e -> {
				File file = FileChooser.forImport("*.zip");
				if (file == null)
					return;
				ProviderType type = ProviderType.of(file);
				IProvider provider = null;
				switch (type) {
				case ILCD_PACKAGE:
					provider = ILCDProvider.of(file);
					break;
				case JSON_LD_PACKAGE:
					provider = JsonProvider.of(file);
					break;
				default:
					break;
				}
				if (provider == null) {
					Error.showBox("Unknown flow source (ILCD "
							+ "or JSON-LD packages are supported).");
					return;
				}
				fireSelect(label, provider);
				syncLink.setVisible(true);
			});

		}

		private void fireSelect(Label label, IProvider provider) {
			label.setText(label(provider));
			label.getParent().pack();
			if (onSelect != null) {
				onSelect.accept(provider);
			}
		}

		private String label(IProvider provider) {
			if (provider == null)
				return "- none -";
			if (provider instanceof DBProvider)
				return "db://" + ((DBProvider) provider).db.getName();
			if (provider instanceof JsonProvider)
				return "jsonld://" + ((JsonProvider) provider).file.getName();
			if (provider instanceof ILCDProvider)
				return "ilcd://" + ((ILCDProvider) provider).file.getName();
			return "?";
		}
	}
}