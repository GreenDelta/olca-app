package org.openlca.app.results.contributions;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.results.UpstreamTree;

class TreeExportDialog extends FormDialog {

	private final UpstreamTree tree;
	private File file;

	private Text maxDepthText;
	private Text minContrText;
	private Text maxRecurText;

	public static int open(UpstreamTree tree) {
		if (tree == null)
			return Window.CANCEL;
		return new TreeExportDialog(tree).open();
	}

	TreeExportDialog(UpstreamTree tree) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.tree = tree;
	}

	@Override
	protected void configureShell(Shell shell) {
		shell.setText(M.ExportToExcel);
		shell.setSize(650, 350);
		UI.center(UI.shell(), shell);
		super.configureShell(shell);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);

		// file selection
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 3, 10, 5);
		var fileText = UI.labeledText(comp, tk, M.ExportToFile);
		fileText.setEditable(false);
		fileText.setBackground(Colors.white());
		var fileBtn = UI.button(comp, tk, M.Browse);
		UI.gridData(fileBtn, false, false).horizontalAlignment = SWT.FILL;
		Controls.onSelect(fileBtn, e -> {
			var f = FileChooser.forSavingFile(
					M.Export, "contribution_tree.xlsx");
			if (f != null) {
				file = f;
				fileText.setText(file.getAbsolutePath());
				var ok = getButton(IDialogConstants.OK_ID);
				if (ok != null) {
					ok.setEnabled(true);
				}
			}
		});

		// number of levels
		UI.gridLayout(comp, 3, 10, 5);
		maxDepthText = UI.labeledText(comp, tk, M.MaxNumberLevels);
		maxDepthText.setText("5");
		var maxDepthBtn = tk.createButton(comp, M.Unlimited, SWT.CHECK);

		// minimum contribution
		minContrText = UI.labeledText(comp, tk, M.MinContribPerc);
		minContrText.setText("1e-5");
		var contrBtn = tk.createButton(comp, M.Unlimited, SWT.CHECK);
		Controls.onSelect(contrBtn, _e -> {
			minContrText.setEnabled(!minContrText.isEnabled());
		});

		// recursion limit
		maxRecurText = UI.labeledText(comp, tk, M.MaxRecursionDepth);
		maxRecurText.setText("1");
		maxRecurText.setEnabled(false);
		UI.label(comp, tk, M.Repetitions);

		Controls.onSelect(maxDepthBtn, _e -> {
			boolean b = !maxDepthText.isEnabled();
			maxDepthText.setEnabled(b);
			maxRecurText.setEnabled(!b);
		});
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		var control = super.createButtonBar(parent);
		// we disable the OK button until a file is selected
		var ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) {
			ok.setEnabled(false);
		}
		return control;
	}

	@Override
	protected void okPressed() {
		if (file == null) {
			MsgBox.error(M.NoFileSelected, M.NoFileSelectedErr);
			return;
		}

		var export = new UpstreamTreeExport(file, tree);

		if (maxDepthText.isEnabled()) {

			// maximum depth
			export.maxRecursionDepth = -1;
			String maxDepth = maxDepthText.getText();
			try {
				export.maxDepth = Integer.parseInt(maxDepth);
				if (export.maxDepth <= 0) {
					MsgBox.error(M.InvalidValue,
							M.InvalidValueForMaxDepth + " - " + maxDepth);
					return;
				}
			} catch (Exception e) {
				MsgBox.error(M.InvalidValue,
						M.InvalidValueForMaxDepth + " - " + maxDepth);
				return;
			}

		} else {

			// no maximum depth => a maximum recursion depth is required
			export.maxDepth = -1;
			String maxRecur = maxRecurText.getText();
			try {
				export.maxRecursionDepth = Integer.parseInt(maxRecur);
				if (export.maxRecursionDepth < 0) {
					MsgBox.error(M.InvalidValue,
							M.InvalidValueForRecursionDepth + " - " + maxRecur);
					return;
				}
			} catch (Exception e) {
				MsgBox.error(M.InvalidValue,
						M.InvalidValueForRecursionDepth + " - " + maxRecur);
				return;
			}

		}

		// minimum contribution
		if (!minContrText.isEnabled()) {
			export.minContribution = -1;
		} else {
			String minContr = minContrText.getText();
			try {
				export.minContribution = Double.parseDouble(minContr) / 100;
				if (export.minContribution <= 0) {
					MsgBox.error(M.InvalidValue,
							M.InvalidValueForMinimumContribution + " - " + minContr);
					return;
				}
			} catch (Exception e) {
				MsgBox.error(M.InvalidValue,
						M.InvalidValueForMinimumContribution + " - " + minContr);
				return;
			}
		}

		// close the dialog and start the export
		super.okPressed();
		App.runWithProgress(M.ExportingContributionTreeDots, export);
	}

}
