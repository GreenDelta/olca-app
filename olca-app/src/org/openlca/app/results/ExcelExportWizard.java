package org.openlca.app.results;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.io.xls.results.system.MatrixPage;
import org.openlca.io.xls.results.system.ResultExport;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

class ExcelExportWizard extends Wizard {

	private final ResultEditor editor;
	private Page page;

	private ExcelExportWizard(ResultEditor editor) {
		this.editor = editor;
		setNeedsProgressMonitor(true);
		setWindowTitle("Export");
	}

	static void openFor(ResultEditor editor) {
		if (editor == null)
			return;
		var wizard = new ExcelExportWizard(editor);
		var dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	@Override
	public void addPages() {
		this.page = new Page(editor);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		var file = page.file;
		if (file == null)
			return false;
		var cache = Cache.getEntityCache();
		var export = new ResultExport(
				editor.setup, editor.result, file, cache)
				.withDqResult(editor.dqResult)
				.withOrder(editor.items)
				.skipZeros(page.skipZeros);
		for (var matrix : page.matrices) {
			export.addPage(matrix);
		}
		try {
			getContainer().run(true, true, monitor -> {
				monitor.beginTask("Exporting file", 1);
				export.run();
				// TODO: forward cancel...
				monitor.done();
			});
			return export.doneWithSuccess();
		} catch (Exception e) {
			ErrorReporter.on("Export failed", e);
			return false;
		}
	}

	private static class Page extends WizardPage {

		private final ResultEditor editor;
		private File file;
		private boolean skipZeros = true;
		private final EnumSet<MatrixPage> matrices = EnumSet.noneOf(MatrixPage.class);

		Page(ResultEditor editor) {
			super("ExcelExportPage");
			this.editor = editor;
			setTitle("Export results to Excel");
			setDescription("Specify an export file and optional settings");
			setImageDescriptor(Icon.EXPORT.descriptor());
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			UI.gridLayout(body, 1);
			setControl(body);
			createFileSelector(body);

			var group = new Group(body, SWT.NONE);
			UI.fillHorizontal(group);
			group.setText("Result matrices (optional)");
			UI.gridLayout(group, 1);
			new Label(group, SWT.NONE).setText(
					"Note that some of these matrices " +
							"can result in very long export times");

			var zeroCheck = UI.checkbox(group, "Skip zero values");
			zeroCheck.setSelection(skipZeros);
			Controls.onSelect(zeroCheck, $ -> skipZeros = zeroCheck.getSelection());

			var all = List.of(
					MatrixPage.DIRECT_INVENTORIES,
					MatrixPage.DIRECT_IMPACTS,
					MatrixPage.FLOW_IMPACTS,
					MatrixPage.TOTAL_INVENTORIES,
					MatrixPage.TOTAL_IMPACTS);
			var forImpacts = EnumSet.of(
					MatrixPage.DIRECT_IMPACTS,
					MatrixPage.FLOW_IMPACTS,
					MatrixPage.TOTAL_IMPACTS);
			var preSelected = EnumSet.of(
					MatrixPage.DIRECT_INVENTORIES,
					MatrixPage.DIRECT_IMPACTS);
			var r = editor.result;
			for (var matrix : all) {
				var matrixCheck = UI.checkbox(group, labelOf(matrix));
				if (!r.hasEnviFlows()
						|| (!r.hasImpacts() && forImpacts.contains(matrix))) {
					matrixCheck.setEnabled(false);
					matrixCheck.setSelection(false);
					continue;
				}
				if (preSelected.contains(matrix)) {
					matrixCheck.setSelection(true);
					matrices.add(matrix);
				}
				Controls.onSelect(matrixCheck, $ -> {
					if (matrixCheck.getSelection()) {
						matrices.add(matrix);
					} else {
						matrices.remove(matrix);
					}
				});
			}
		}

		private String labelOf(MatrixPage matrix) {
			return switch (matrix) {
				case DIRECT_INVENTORIES -> "Direct inventory contributions";
				case TOTAL_INVENTORIES -> "Upstream inventories";
				case DIRECT_IMPACTS -> "Direct impact contributions";
				case TOTAL_IMPACTS -> "Upstream impacts";
				case FLOW_IMPACTS -> "Impacts by flow";
			};
		}

		private void createFileSelector(Composite body) {
			var comp = new Composite(body, SWT.NONE);
			UI.fillHorizontal(comp);
			UI.gridLayout(comp, 3);

			var text = UI.labeledText(comp, M.File, SWT.READ_ONLY);
			text.setBackground(Colors.systemColor(SWT.COLOR_LIST_BACKGROUND));
			if (file != null) {
				text.setText(file.getName());
			}
			UI.fillHorizontal(text);

			var browse = new Button(comp, SWT.NONE);
			browse.setText(M.Browse);
			browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

			Controls.onSelect(browse, e -> {
				String fileName = Labels.name(editor.setup.target())
						.replaceAll("[^A-Za-z\\d]", "_") + ".xlsx";
				var f = FileChooser.forSavingFile(M.Export, fileName);
				if (f != null) {
					this.file = f;
					setPageComplete(true);
					text.setText(f.getName());
				}
			});
		}
	}
}
