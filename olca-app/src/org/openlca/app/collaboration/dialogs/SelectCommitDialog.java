package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.collaboration.viewers.HistoryViewer;
import org.openlca.app.db.Repository;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.git.model.Commit;

public class SelectCommitDialog extends FormDialog {

	private HistoryViewer viewer;
	private Commit selection;

	public SelectCommitDialog() {
		super(UI.shell());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var body = UI.formBody(mform.getForm(), mform.getToolkit());
		viewer = new HistoryViewer(body);
		UI.gridData(viewer.getViewer().getTable(), true, true);
		Tables.bindColumnWidths(viewer.getViewer(), 0.1, 0.7, 0.1, 0.1);
		viewer.addSelectionChangedListener((e) -> {
			selection = Viewers.getFirstSelected(viewer.getViewer());
			updateButtons();
		});
		viewer.setRepository(Repository.get());
	}

	private void updateButtons() {
		var button = getButton(IDialogConstants.OK_ID);
		button.setEnabled(selection != null);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	public Commit getSelection() {
		return selection;
	}

}
