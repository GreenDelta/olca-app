package org.openlca.app.results;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProcessGroupSet;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The action for opening a grouping set from the database in the grouping page
 * of the analysis editor.
 */
class GroupSetAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private GroupPage page;

	public GroupSetAction(GroupPage page) {
		this.page = page;
		setToolTipText(Messages.Open);
		setImageDescriptor(ImageType.FOLDER_ICON_OPEN.getDescriptor());
	}

	@Override
	public void run() {
		Shell shell = UI.shell();
		GroupDialog dialog = new GroupDialog(shell);
		if (Window.OK == dialog.open()) {
			ProcessGroupSet set = dialog.selectedGrouping;
			if (set != null)
				page.applyGrouping(set);
		}
	}

	private class GroupDialog extends Dialog implements SelectionListener {

		private FormToolkit toolkit;
		private ProcessGroupSet[] groupSets;
		private Combo combo;
		private ProcessGroupSet selectedGrouping;

		public GroupDialog(Shell parentShell) {
			super(parentShell);
			toolkit = new FormToolkit(parentShell.getDisplay());
			groupSets = getGroupSets();
		}

		private ProcessGroupSet[] getGroupSets() {
			try {
				List<ProcessGroupSet> list = Database.createDao(
						ProcessGroupSet.class).getAll();
				ProcessGroupSet[] groups = list
						.toArray(new ProcessGroupSet[list.size()]);
				Arrays.sort(groups, (s1, s2) ->
						Strings.compare(s1.getName(), s1.getName()));
				return groups;
			} catch (Exception e) {
				log.error("Failed to load the grouping sets", e);
				return new ProcessGroupSet[0];
			}
		}

		@Override
		protected Control createDialogArea(Composite root) {
			getShell().setText(Messages.Open);
			toolkit.adapt(root);
			Composite area = (Composite) super.createDialogArea(root);
			toolkit.adapt(area);
			Composite container = toolkit.createComposite(area);
			UI.gridData(container, true, true);
			UI.gridLayout(container, 2);
			combo = UI.formCombo(container, toolkit, Messages.Grouping);
			UI.gridData(combo, false, false).widthHint = 250;
			combo.addSelectionListener(this);
			getShell().pack();
			UI.center(getParentShell(), getShell());
			bindData();
			return area;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			selectedGrouping = null;
			if (combo == null || groupSets == null)
				return;
			int idx = combo.getSelectionIndex();
			if (idx < 0 || idx > (groupSets.length - 1))
				return;
			selectedGrouping = groupSets[idx];
		}

		private void bindData() {
			for (ProcessGroupSet set : groupSets) {
				combo.add(set.getName());
			}
			if (groupSets.length > 0) {
				combo.select(0);
				selectedGrouping = groupSets[0];
			}
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			toolkit.adapt(parent);
			createButton(parent, IDialogConstants.OK_ID,
					IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID,
					IDialogConstants.CANCEL_LABEL, false);
			getShell().pack();
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 300);
		}

		@Override
		public boolean close() {
			if (toolkit != null)
				toolkit.dispose();
			return super.close();
		}
	}

}
