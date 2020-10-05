package org.openlca.app.cloud.ui.commits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.cloud.model.data.Commit;

class HistoryViewer extends AbstractTableViewer<Commit> {

	HistoryViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new HistoryLabel();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.Id, M.Message, M.Committer, M.CommitDate };
	}

	@Override
	protected List<Action> getAdditionalActions() {
		List<Action> actions = new ArrayList<>();
		actions.add(new CheckoutAction(this));
		actions.add(new OpenCompareViewAction(this));
		return actions;
	}
	
	List<Commit> getCommits() {
		Object input = getViewer().getInput();
		if (input instanceof Object[])
			return null;
		return Arrays.asList((Commit[]) getViewer().getInput());
	}

	class HistoryLabel extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider, ITableFontProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int column) {
			Commit commit = (Commit) element;
			switch (column) {
			case 0:
				return commit.id;
			case 1:
				return commit.message;
			case 2:
				return commit.user;
			case 3:
				return CloudUtil.formatCommitDate(commit.timestamp);
			}
			return null;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			Commit commit = (Commit) element;
			String lastCommitId = Database.getRepositoryClient().getConfig().getLastCommitId();
			if (!commit.id.equals(lastCommitId))
				return null;
			return UI.boldFont();
		}

	}

}