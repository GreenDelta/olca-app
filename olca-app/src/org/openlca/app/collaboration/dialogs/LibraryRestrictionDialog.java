package org.openlca.app.collaboration.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.model.LibraryRestriction;
import org.openlca.app.collaboration.model.LibraryRestriction.RestrictionType;
import org.openlca.app.collaboration.viewers.LibraryRestrictionViewer;
import org.openlca.app.util.UI;

public class LibraryRestrictionDialog extends FormDialog {

	private final List<LibraryRestriction> restrictions;

	public LibraryRestrictionDialog(List<LibraryRestriction> restrictions) {
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
		var form = UI.formHeader(mform, M.LibraryDataSets);
		var toolkit = mform.getToolkit();
		var body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		var description = M.RecognizedLibraryDatasetsDescription;
		var label = toolkit.createLabel(body, description, SWT.WRAP);
		UI.gridData(label, true, false).widthHint = 750;
		var viewer = new LibraryRestrictionViewer(body);
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
