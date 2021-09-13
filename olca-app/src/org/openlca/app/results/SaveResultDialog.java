package org.openlca.app.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Categories;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.results.SystemProcess;
import org.openlca.util.ResultModels;

public class SaveResultDialog extends FormDialog {

	private final ResultEditor<?> editor;
	private Text nameText;
	private Button resultRadio;
	private Button processRadio;
	private Button metaCheck;

	public static void open(ResultEditor<?> editor) {
		if (editor == null)
			return;
		new SaveResultDialog(editor).open();
	}

	private SaveResultDialog(ResultEditor<?> editor) {
		super(UI.shell());
		this.editor = editor;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Save result");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 350);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);

		// name
		var nameComp = tk.createComposite(body);
		UI.gridLayout(nameComp, 2);
		UI.fillHorizontal(nameComp);
		nameText = UI.formText(nameComp, tk, "Name");
		Controls.onPainted(nameText, () -> {
			var name = Labels.name(editor.setup.target());
			if (name != null) {
				nameText.setText(name);
			}
		});

		var groupComp = tk.createComposite(body);
		UI.gridLayout(groupComp, 1, 0, 0);
		UI.fillHorizontal(groupComp);

		var resultGroup = createGroup(groupComp, tk);
		resultRadio = tk.createButton(
			resultGroup, "As result model", SWT.RADIO);
		resultRadio.setSelection(true);

		var processGroup = createGroup(groupComp, tk);
		processRadio = tk.createButton(
			processGroup, "As sytem process", SWT.RADIO);
		processRadio.setSelection(false);

		metaCheck = tk.createButton(
			processGroup, M.CopyMetaDataFromReferenceProcess, SWT.CHECK);
		metaCheck.setSelection(true);
		metaCheck.setEnabled(false);

		Controls.onSelect(processRadio, $ -> {
			resultRadio.setSelection(false);
			metaCheck.setEnabled(true);
		});
		Controls.onSelect(resultRadio, $ -> {
			processRadio.setSelection(false);
			metaCheck.setEnabled(false);
		});

	}

	private Group createGroup(Composite parent, FormToolkit tk) {
		var group = new Group(parent, SWT.NONE);
		UI.fillHorizontal(group);
		UI.gridLayout(group, 1);
		tk.adapt(group);
		return group;
	}

	@Override
	protected void okPressed() {
		var exec = new Exec(nameText.getText(), editor)
			.createProcess(processRadio.getSelection())
			.withMetaData(metaCheck.getSelection());
		var label = processRadio.getSelection()
			? "Save as system process ..."
			: "Save as result model ...";
		var entity = App.exec(label, exec::run);
		App.open(entity);
		Navigator.refresh();
		super.okPressed();
	}

	private static class Exec {

		final String name;
		final ResultEditor<?> editor;
		boolean createProcess;
		boolean withMetaData;

		Exec(String name, ResultEditor<?> editor) {
			this.name = name;
			this.editor = editor;
		}

		Exec createProcess(boolean b) {
			this.createProcess = b;
			return this;
		}

		Exec withMetaData(boolean b) {
			this.withMetaData = b;
			return this;
		}

		CategorizedEntity run() {
			var db = Database.get();
			if (createProcess) {
				var process = withMetaData
					? SystemProcess.createWithMetaData(
					db, editor.setup, editor.result, name)
					: SystemProcess.create(
					db, editor.setup, editor.result, name);
				process.category = Categories.removeLibraryFrom(process.category);
				return db.insert(process);
			}

			var result = ResultModels.createFrom(
					db, editor.setup, editor.result);
			result.name = name;
			return db.insert(result);
		}
	}
}
