package org.openlca.app.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.window.Window;
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
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Process;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.SystemProcess;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaveProcessDialog extends Wizard {

	private final IResultEditor<? extends ContributionResultProvider<?>> editor;
	private Page page;

	private SaveProcessDialog(IResultEditor<?> editor) {
		this.editor = editor;
		setNeedsProgressMonitor(true);
	}

	public static int open(IResultEditor<?> editor) {
		if (editor == null)
			return Window.CANCEL;
		SaveProcessDialog d = new SaveProcessDialog(editor);
		d.setWindowTitle(M.SaveAsLCIResult);
		WizardDialog dialog = new WizardDialog(UI.shell(), d);
		dialog.setPageSize(150, 250);
		return dialog.open();
	}

	@Override
	public boolean performFinish() {
		String name = page.nameText.getText();
		if (Strings.nullOrEmpty(name)) {
			Error.showBox(M.NameCannotBeEmpty);
			return false;
		}
		boolean createMeta = page.metaCheck.getSelection();
		try {
			getContainer().run(true, false, m -> {
				m.beginTask(M.CreateProcess, IProgressMonitor.UNKNOWN);
				Process p = null;
				if (createMeta) {
					p = SystemProcess.createWithMetaData(
							Database.get(), editor.getSetup(), editor.getResult(), name);
				} else {
					p = SystemProcess.create(
							Database.get(), editor.getSetup(), editor.getResult(), name);
				}
				ProcessDao dao = new ProcessDao(Database.get());
				p = dao.insert(p);
				App.openEditor(p);
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
			nameText.setText(editor.getSetup().productSystem.getName() + " - LCI");
			UI.filler(parent);
			metaCheck = UI.checkBox(parent, M.CopyMetaDataFromReferenceProcess);
			metaCheck.setSelection(true);
		}
	}
}
