package org.openlca.app.wizards.io;

import java.io.File;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.xls.process.XlsProcessReader;
import org.openlca.jsonld.input.UpdateMode;

public class ExcelImportWizard extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public ExcelImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.Import);
		setDefaultPageImageDescriptor(Icon.IMPORT_WIZARD.descriptor());
	}

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Wizards.forImport("wizard.import.excel", (ExcelImportWizard w) -> {
			w.initialFile = file;
			if (w.page != null) {
				w.page.files.add(file);
				w.page.setPageComplete(true);
			}
		});
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		if (Database.isNoneActive()) {
			addPage(new NoDatabaseErrorPage());
			return;
		}
		page = new Page(initialFile);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		var files = page.files;
		if (files.isEmpty())
			return false;
		try {
			Database.getWorkspaceIdUpdater().beginTransaction();
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(M.Import, files.size());
				var reader = XlsProcessReader.of(Database.get())
						.withUpdates(page.updateMode());
				for (var file : files) {
					monitor.subTask(file.getName());
					reader.sync(file);
					monitor.worked(1);
				}
				monitor.done();
			});
			return true;
		} catch (Exception e) {
			ErrorReporter.on("Failed to import Excel file(s)", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private static class Page extends WizardPage {

		List<File> files;
		private final boolean[] _updateMode = {false, true, false};

		Page(File initial) {
			super("ExcelImportWizard.Page");
			setTitle("Import processes from Excel files");
			setDescription(
					"Note that only files in the openLCA process format are supported");
			files = initial != null
					? List.of(initial)
					: List.of();
			setPageComplete(!files.isEmpty());
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			UI.gridLayout(body, 1);

			FilePanel.on(files -> {
						this.files = files;
						setPageComplete(!files.isEmpty());
					})
					.withTitle("Selected one or more Excel files")
					.withExtensions("*.xlsx")
					.withFiles(files)
					.render(body);

			var group = UI.group(body);
			group.setText("When a process with an ID already exists");
			UI.fillHorizontal(group);
			UI.gridLayout(group, 1);
			for (int i = 0; i < _updateMode.length; i++) {
				int id = i;
				var b = new Button(group, SWT.RADIO);
				b.setText(updateLabel(i));
				b.setSelection(_updateMode[i]);
				Controls.onSelect(b, e -> {
					for (int j = 0; j < _updateMode.length; j++) {
						_updateMode[j] = id == j;
					}
				});
			}

			setControl(body);
		}

		private String updateLabel(int i) {
			return switch (i) {
				case 0 -> "Keep the version in the database";
				case 1 -> "Update it in the database if it is newer";
				case 2 -> "Always update it in the database";
				default -> "?";
			};
		}

		private UpdateMode updateMode() {
			var order = new UpdateMode[]{
					UpdateMode.NEVER,
					UpdateMode.IF_NEWER,
					UpdateMode.ALWAYS
			};
			for (int i = 0; i < order.length; i++) {
				if (_updateMode[i]) {
					return order[i];
				}
			}
			return UpdateMode.IF_NEWER;
		}
	}
}
