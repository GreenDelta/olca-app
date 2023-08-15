package org.openlca.app.results;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
				export.run();
				// TODO: forward cancel...
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
