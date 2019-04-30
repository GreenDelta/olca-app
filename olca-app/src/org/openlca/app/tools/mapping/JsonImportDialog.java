package org.openlca.app.tools.mapping;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMap;
import org.openlca.app.tools.mapping.model.FlowMaps;
import org.openlca.app.tools.mapping.model.JsonProvider;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsonImportDialog extends Dialog {

	private final File file;
	private final JsonProvider provider;
	private final List<FlowMap> flowMaps;

	/**
	 * The selected flow map in the dialog that should be applied. If this is null
	 * it means that a flow map should be generated from the flow attributes.
	 */
	private FlowMap selectedMap;

	static void open(File file) {
		if (file == null)
			return;
		try {
			JsonImportDialog d = new JsonImportDialog(file);
			if (d.open() != Dialog.OK) {
				d.provider.close();
				return;
			}
			if (d.selectedMap == null) {
				Info.showBox("Not yet implemented.");
				return;
			}
			FlowMap flowMap = d.selectedMap;
			App.runWithProgress("Sync flow map ...",
					() -> FlowMaps.sync(flowMap, d.provider.store, Database.get()),
					() -> MappingTool.open(flowMap, d.provider));
		} catch (Exception e) {
			Error.showBox("Failed to open file as JSON-LD package");
			Logger log = LoggerFactory.getLogger(JsonImportDialog.class);
			log.error("failed to open JSON-LD package " + file, e);
		}
	}

	JsonImportDialog(File file) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.provider = new JsonProvider(file);
		this.file = file;
		this.flowMaps = provider.getFlowMaps();
		Collections.sort(this.flowMaps,
				(m1, m2) -> Strings.compare(m1.name, m2.name));
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Open flow mapping");
		UI.center(UI.shell(), shell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite root = (Composite) super.createDialogArea(parent);
		UI.gridLayout(root, 1, 10, 10);

		Composite comp = new Composite(root, SWT.NONE);
		UI.gridLayout(comp, 2, 10, 0);
		UI.formLabel(comp, M.File);
		String fileText = Strings.cut(file.getParent(), 50)
				+ File.separator
				+ Strings.cut(file.getName(), 50);
		Label label = UI.formLabel(comp, fileText);
		label.setForeground(Colors.linkBlue());

		Button openCheck = new Button(root, SWT.RADIO);
		openCheck.setText("Open mapping definition");
		Combo combo = new Combo(root, SWT.READ_ONLY);
		Controls.onSelect(combo, e -> {
			selectedMap = flowMaps.get(combo.getSelectionIndex());
		});
		UI.gridData(combo, true, false);
		Controls.onSelect(openCheck, e -> {
			selectedMap = flowMaps.get(combo.getSelectionIndex());
			combo.setEnabled(true);
		});

		Button genCheck = new Button(root, SWT.RADIO);
		genCheck.setText("Generate mapping based on flow attributes");
		Controls.onSelect(genCheck, e -> {
			selectedMap = null;
			combo.setEnabled(false);
		});

		if (flowMaps.isEmpty()) {
			genCheck.setSelection(true);
			openCheck.setSelection(false);
			combo.setEnabled(false);
			openCheck.setEnabled(false);
		} else {
			openCheck.setSelection(true);
			genCheck.setSelection(false);
			String[] items = this.flowMaps
					.stream()
					.map(m -> m.name)
					.toArray(len -> new String[len]);
			combo.setItems(items);
			combo.select(0);
			selectedMap = flowMaps.get(0);
		}
		return root;
	}
}
