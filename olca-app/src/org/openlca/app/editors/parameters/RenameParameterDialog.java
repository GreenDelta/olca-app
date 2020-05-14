package org.openlca.app.editors.parameters;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.usage.ParameterUsageTree;
import org.openlca.core.model.Parameter;
import org.openlca.util.Parameters;
import org.openlca.util.Strings;

public class RenameParameterDialog extends FormDialog {

	private final ParameterUsageTree usageTree;
	private final Parameter param;
	private final List<Parameter> otherParams;

	private RenameParameterDialog(
			Parameter param, ParameterUsageTree usageTree) {
		super(UI.shell());
		this.param = param;
		this.usageTree = usageTree;
		this.otherParams = new ParameterDao(Database.get())
				.getGlobalParameters()
				.stream()
				.filter(other -> !Objects.equals(param, other))
				.collect(Collectors.toList());
	}

	public static void open(Parameter param) {
		if (param == null)
			return;
		var db = Database.get();
		var tree = new AtomicReference<ParameterUsageTree>();
		App.run(
				"Collect dependencies",
				() -> tree.set(ParameterUsageTree.build(param.name, db)),
				() -> new RenameParameterDialog(param, tree.get()).open());
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		UI.center(UI.shell(), shell);
		shell.setText("Rename parameter " + param.name);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2, 10, 0);
		var text = UI.formText(comp, tk, "New name");
		if (param.name != null) {
			text.setText(param.name);
		}
		text.addModifyListener(e -> {
			var name = text.getText().trim();
			var err = check(name);
			if (err != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				text.setBackground(Colors.errorColor());
				text.setToolTipText(err);
			} else {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
				text.setBackground(Colors.white());
				text.setToolTipText("");
			}
		});
	}

	private String check(String name) {
		if (Strings.nullOrEmpty(name))
			return M.NameCannotBeEmpty;
		if (!Parameters.isValidName(name))
			return M.InvalidParameterName;
		var _name = name.trim().toLowerCase();
		for (var other : otherParams) {
			if (Strings.nullOrEmpty(other.name))
				continue;
			var otherName = other.name.trim().toLowerCase();
			if (Strings.nullOrEqual(_name, otherName))
				return M.ParameterWithSameNameExists;
		}
		return null;
	}

	@Override
	protected void okPressed() {


		super.okPressed();
	}
}
