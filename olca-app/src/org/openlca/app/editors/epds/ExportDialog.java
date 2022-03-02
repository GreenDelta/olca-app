package org.openlca.app.editors.epds;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.tools.openepd.model.Ec3InternalEpd;
import org.openlca.app.util.UI;

import java.util.Objects;

class ExportDialog extends FormDialog  {

	private final Ec3InternalEpd epd;

	public static void show(Ec3InternalEpd epd) {
		if (epd == null)
			return;


	}

	private ExportDialog(Ec3InternalEpd epd) {
		super(UI.shell());
		this.epd = Objects.requireNonNull(epd);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export an openEPD document");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
	}
}
