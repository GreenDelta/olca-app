package org.openlca.app.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.util.Categories;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.results.SystemProcess;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated will be removed when we switch to the new `SaveResultDialog`
 */
@Deprecated
public final class SaveProcessDialog extends Wizard {

	private final ResultEditor<?> editor;
	private Page page;

	private SaveProcessDialog(ResultEditor<?> editor) {
		this.editor = editor;
		setNeedsProgressMonitor(true);
	}

	public static void open(ResultEditor<?> editor) {
		if (editor == null)
			return;
		// TODO
		if (FeatureFlag.RESULTS.isEnabled()) {
			SaveResultDialog.open(editor);
			return;
		}
		var d = new SaveProcessDialog(editor);
		d.setWindowTitle(M.SaveAsLCIResult);
		var dialog = new WizardDialog(UI.shell(), d);
		dialog.setPageSize(150, 250);
		dialog.open();
	}

	@Override
	public boolean performFinish() {
		String name = page.nameText.getText();
		if (Strings.nullOrEmpty(name)) {
			MsgBox.error(M.NameCannotBeEmpty);
			return false;
		}
		boolean createMeta = page.metaCheck.getSelection();
		try {
			getContainer().run(true, false, m -> {
				m.beginTask(M.CreateProcess, IProgressMonitor.UNKNOWN);
				var db = Database.get();
				var process = createMeta
					? SystemProcess.createWithMetaData(
					db, editor.setup, editor.result, name)
					: SystemProcess.create(
					db, editor.setup, editor.result, name);
				process.category = Categories.removeLibraryFrom(process.category);
				process = db.insert(process);
				App.open(process);
				m.done();
			});
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to create process from result", e);
			return false;
		}
	}

	@Override
	public void addPages() {
		page = new Page();
		addPage(page);
	}

	private class Page extends WizardPage {

		private Text nameText;
		private Button metaCheck;

		private Page() {
			super("SaveProcessDialogPage", M.SaveAsLCIResult, null);
			setPageComplete(true);
		}

		@Override
		public void createControl(Composite root) {
			Composite parent = new Composite(root, SWT.NONE);
			setControl(parent);
			UI.gridLayout(parent, 2);
			nameText = UI.formText(parent, M.Name);
			nameText.setText(Labels.name(editor.setup.target()) + " - LCI");
			UI.filler(parent);
			metaCheck = UI.checkBox(parent, M.CopyMetaDataFromReferenceProcess);
			metaCheck.setSelection(true);
		}
	}
}
