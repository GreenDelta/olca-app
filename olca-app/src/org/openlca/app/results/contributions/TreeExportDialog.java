package org.openlca.app.results.contributions;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.core.results.UpstreamTree;

class TreeExportDialog extends FormDialog {

	public static int open(UpstreamTree tree) {
		if (tree == null)
			return Window.CANCEL;
		return new TreeExportDialog(tree).open();
	}

	TreeExportDialog(UpstreamTree tree) {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell shell) {
		shell.setText(M.ExportToExcel);
		shell.setSize(650, 350);
		UI.center(UI.shell(), shell);
		super.configureShell(shell);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);

		// file selection
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 3, 10, 5);
		var fileText = UI.formText(comp, tk, "Export to file");
		fileText.setEditable(false);
		fileText.setBackground(Colors.white());
		var fileBtn = tk.createButton(comp, M.Browse, SWT.NONE);
		UI.gridData(fileBtn, false, false).horizontalAlignment = SWT.FILL;

		// number of levels
		UI.gridLayout(comp, 3, 10, 5);
		var levelText = UI.formText(comp, tk, "Max. number of levels");
		tk.createButton(comp, "Unlimited", SWT.CHECK);

		// recursion limit
		UI.formText(comp, tk, "Max. recursion depth").setEnabled(false);
		UI.formLabel(comp, tk, "Repetitions");
		UI.formText(comp, tk, "Min. recursion contribution").setEnabled(false);
		UI.formLabel(comp, tk, "%");
	}

	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
		super.okPressed();
	}

}
