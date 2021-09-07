package org.openlca.app.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.util.UI;

public class SaveResultDialog extends FormDialog {

	public static void open(ResultEditor<?> editor) {
		if (editor == null)
			return;
		new SaveResultDialog().open();
	}

	private SaveResultDialog() {
		super(UI.shell());
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
		var nameText =  UI.formText(nameComp, tk, "Name");

		var groupComp = tk.createComposite(body);
		UI.gridLayout(groupComp, 1, 0, 0);
		UI.fillHorizontal(groupComp);

		var resultGroup = new Group(groupComp, SWT.NONE);
		UI.fillHorizontal(resultGroup);
		UI.gridLayout(resultGroup, 1);
		tk.adapt(resultGroup);
		var resultCheck = tk.createButton(
			resultGroup, "As result model", SWT.RADIO);
		resultCheck.setSelection(true);

		var processGroup = new Group(groupComp, SWT.NONE);
		UI.fillHorizontal(processGroup);
		UI.gridLayout(processGroup, 1);
		tk.adapt(processGroup);
		var processCheck = tk.createButton(
				processGroup, "As sytem process", SWT.RADIO);
		processCheck.setSelection(false);

		var metaCheck = tk.createButton(
				processGroup,  M.CopyMetaDataFromReferenceProcess, SWT.CHECK);
		metaCheck.setSelection(true);
		metaCheck.setEnabled(false);

	}
}
