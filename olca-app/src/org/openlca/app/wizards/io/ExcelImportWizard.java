package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
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
		Wizards.forImport("wizard.import.excel",
				(ExcelImportWizard w) -> {
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
		page = new Page();
		if (initialFile != null) {
			page.files.add(initialFile);
			page.setPageComplete(true);
		}
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

		final List<File> files = new ArrayList<>();
		private final boolean[] _updateMode = {false, true, false};

		Page() {
			super("ExcelImportWizard.Page");
			setTitle("Import processes from Excel files");
			setDescription(
					"Note that only files in the openLCA process format are supported");
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var body = new Composite(parent, SWT.NONE);
			UI.gridLayout(body, 1);
			var link = new Hyperlink(body, SWT.NONE);
			link.setText("Selected one or more Excel files:");
			link.setForeground(Colors.linkBlue());

			var viewer = Tables.createViewer(body, M.File);
			var table = viewer.getTable();
			table.setHeaderVisible(false);
			table.setLinesVisible(false);
			Tables.bindColumnWidths(table, 1.0);
			viewer.setLabelProvider(new FileLabel());
			viewer.setInput(files);

			var addFiles = Actions.create(M.Add, Icon.ADD.descriptor(), () -> {
				var next = FileChooser.openFile()
						.withExtensions("*.xlsx")
						.withTitle("Select one or more Excel files...")
						.selectMultiple();
				if (next.isEmpty())
					return;
				for (var f : next) {
					if (!files.contains(f)) {
						files.add(f);
					}
				}
				viewer.setInput(files);
				setPageComplete(!files.isEmpty());
			});

			var removeFiles = Actions.create(M.Remove, Icon.DELETE.descriptor(), () -> {
				List<File> removals = Viewers.getAllSelected(viewer);
				if (removals.isEmpty())
					return;
				files.removeAll(removals);
				viewer.setInput(files);
				setPageComplete(!files.isEmpty());
			});

			Actions.bind(viewer, addFiles, removeFiles);

			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					addFiles.run();
				}

				@Override
				public void linkEntered(HyperlinkEvent e) {
					link.setUnderlined(true);
				}

				@Override
				public void linkExited(HyperlinkEvent e) {
					link.setUnderlined(false);
				}
			});

			var label = new Label(body, SWT.NONE);
			label.setText("When a process with an ID already exists:");
			var gd = UI.gridData(label, false, false);
			gd.verticalAlignment = SWT.TOP;
			gd.verticalIndent = 2;

			for (int i = 0; i < _updateMode.length; i++) {
				int id = i;
				var b = new Button(body, SWT.RADIO);
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

	private static class FileLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Images.get(FileType.EXCEL);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof File file
					? file.getAbsolutePath()
					: null;
		}
	}
}
