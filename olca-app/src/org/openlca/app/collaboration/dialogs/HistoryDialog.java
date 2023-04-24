package org.openlca.app.collaboration.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.collaboration.util.Format;
import org.openlca.app.db.Repository;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.git.model.Commit;

public class HistoryDialog extends FormDialog {

	private final String title;
	private final List<Commit> commits;

	public HistoryDialog(String title, List<Commit> commits) {
		super(UI.shell());
		this.title = title;
		this.commits = commits;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform, title);
		var body = UI.body(form, mform.getToolkit());
		var viewer = new AbstractTableViewer<Commit>(body) {
			@Override
			protected IBaseLabelProvider getLabelProvider() {
				return new CommitLabelProvider();
			}
		};
		viewer.getViewer().addDoubleClickListener(this::onDoubleClick);
		form.reflow(true);
		viewer.setInput(commits);
	}

	private void onDoubleClick(DoubleClickEvent event) {
		var repo = Repository.get();
		if (repo == null || !repo.isCollaborationServer())
			return;
		Commit element = Selections.firstOf(event);
		var url = repo.url() + "/" + "commit" + "/" + element.id;
		Desktop.browse(url);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	private class CommitLabelProvider extends BaseLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof Commit))
				return null;
			return getCommitText((Commit) element);
		}

		private String getCommitText(Commit commit) {
			var text = commit.user + ": ";
			text += commit.message + " (";
			text += Format.commitDate(commit.timestamp) + ")";
			return text;
		}

	}
}
