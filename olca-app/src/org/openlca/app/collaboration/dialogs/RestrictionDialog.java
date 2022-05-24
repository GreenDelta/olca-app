package org.openlca.app.collaboration.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.collaboration.model.Restriction;
import org.openlca.app.collaboration.model.Restriction.RestrictionType;
import org.openlca.app.collaboration.viewers.RestrictionViewer;
import org.openlca.app.util.UI;

public class RestrictionDialog extends FormDialog {

	private final List<Restriction> restrictions;

	public RestrictionDialog(List<Restriction> restrictions) {
		super(UI.shell());
		this.restrictions = restrictions;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	public int open() {
		return super.open();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, "Restricted data sets");
		var toolkit = mform.getToolkit();
		var body = UI.formBody(form, toolkit);
		var description = "Some of the selected data sets were identified as restricted data sets (e.g. may underlie license restrictions). If you are allowed to commit changes to these data sets, they are marked with a yellow warning sign, otherwise with a red forbidden sign.";
		var label = toolkit.createLabel(body, description, SWT.WRAP);
		UI.gridData(label, true, false).widthHint = 750;
		var viewer = new RestrictionViewer(body);
		form.reflow(true);
		viewer.setInput(restrictions);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (containsForbidden()) {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}

	private boolean containsForbidden() {
		for (var restriction : restrictions)
			if (restriction.type == RestrictionType.FORBIDDEN)
				return true;
		return false;
	}

}
