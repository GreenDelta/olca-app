package org.openlca.app.cloud.ui.library;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.LibraryRestriction;
import org.openlca.cloud.model.RestrictionType;

public class LibraryResultDialog extends FormDialog {

	private final List<LibraryRestriction> restrictions;

	public LibraryResultDialog(List<LibraryRestriction> restrictions) {
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
		ScrolledForm form = UI.formHeader(mform, M.LibraryDataSets);
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		String description = M.RecognizedLibraryDatasetsDescription;
		Label label = toolkit.createLabel(body, description, SWT.WRAP);
		UI.gridData(label, true, false).widthHint = 750;
		Viewer viewer = new Viewer(body);
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
		for (LibraryRestriction restriction : restrictions) 
			if (restriction.type == RestrictionType.FORBIDDEN)
				return true;
		return false;
	}

}
