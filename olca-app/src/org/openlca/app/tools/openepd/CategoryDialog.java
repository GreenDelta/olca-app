package org.openlca.app.tools.openepd;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.tools.openepd.model.Ec3Category;
import org.openlca.app.tools.openepd.model.Ec3CategoryTree;
import org.openlca.app.util.UI;

class CategoryDialog extends FormDialog {

	private final Ec3CategoryTree categories;
	private Ec3Category selected;

	public static Ec3Category selectFrom(Ec3CategoryTree tree) {
		if (tree == null || tree.isEmpty())
			return null;
		var dialog = new CategoryDialog(tree);
		return dialog.open() == Window.OK
			? dialog.selected
			: null;
	}

	private CategoryDialog(Ec3CategoryTree categories) {
		super(UI.shell());
		this.categories = categories;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select a category");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 800, 700);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
	}
}
