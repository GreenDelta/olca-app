package org.openlca.app.cloud.ui.commits;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.cloud.WebRequestExceptions;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.util.WebRequests.WebRequestException;

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
		Composite body = UI.formBody(mform.getForm(), mform.getToolkit());
		viewer = new HistoryViewer(body);
		UI.gridData(viewer.getViewer().getTable(), true, true);
		Tables.bindColumnWidths(viewer.getViewer(), 0.1, 0.7, 0.1, 0.1);
		viewer.addSelectionChangedListener((e) -> {
			selection = Viewers.getFirstSelected(viewer.getViewer());
			updateButtons();
		});
		setContent();
	}

	private void setContent() {
		if (!Database.isConnected()) {
			viewer.setInput(new Commit[0]);
			return;
		}
		RepositoryClient client = Database.getRepositoryClient();
		try {
			viewer.setInput(client.fetchCommitHistory());
		} catch (WebRequestException e) {
			WebRequestExceptions.handle(e);
		}
		return;
	}

	@SuppressWarnings("unchecked")
	public List<Commit> getCommits() {
		return (List<Commit>) viewer.getViewer().getInput();
	}

	private void updateButtons() {
		Button button = getButton(IDialogConstants.OK_ID);
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
