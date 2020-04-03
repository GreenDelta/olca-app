package org.openlca.app.cloud.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.cloud.api.RepositoryConfig;

public class ConfigureRepositoriesDialog extends FormDialog {

	private RepositoryConfigViewer viewer;

	public ConfigureRepositoriesDialog() {
		super(UI.shell());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		FormToolkit toolkit = mform.getToolkit();
		UI.formHeader(mform, M.ConfigureRepositories);
		Composite body = UI.formBody(form, toolkit);
		viewer = new RepositoryConfigViewer(body);
		Tables.bindColumnWidths(viewer.getViewer(), 0.4, 0.3, 0.2, 0.1);
		viewer.setInput(RepositoryConfig.loadAll(Database.get()));
		Actions.bind(viewer.getViewer(), new AddAction(), new RemoveAction());
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	private class AddAction extends Action {

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.ADD.descriptor();
		}

		@Override
		public String getText() {
			return M.Add;
		}

		@Override
		public void run() {
			AddRepositoryDialog dialog = new AddRepositoryDialog();
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			dialog.saveConfig();
			viewer.setInput(RepositoryConfig.loadAll(Database.get()));
		}

	}

	private class RemoveAction extends Action {

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.DELETE.descriptor();
		}

		@Override
		public String getText() {
			return M.Remove;
		}

		@Override
		public void run() {
			RepositoryConfig config = viewer.getSelected();
			config.remove();
			viewer.setInput(RepositoryConfig.loadAll(Database.get()));
		}

		@Override
		public boolean isEnabled() {
			return viewer.getSelected() != null;
		}

	}

	private static class RepositoryConfigViewer extends AbstractTableViewer<RepositoryConfig> {

		private static final String COLUMN_SERVER_URL = M.ServerUrl;
		private static final String COLUMN_REPOSITORY = M.Repository;
		private static final String COLUMN_USER = M.User;
		private static final String COLUMN_ACTIVE = M.Active;
		private static final String[] COLUMNS = { COLUMN_SERVER_URL, COLUMN_REPOSITORY, COLUMN_USER, COLUMN_ACTIVE };

		protected RepositoryConfigViewer(Composite parent) {
			super(parent);
			getModifySupport().bind(COLUMN_ACTIVE, new CheckBoxCellModifier<RepositoryConfig>() {

				@Override
				protected boolean isChecked(RepositoryConfig element) {
					return element.isActive();
				}

				@Override
				protected void setChecked(RepositoryConfig element, boolean value) {
					if (value) {
						element.activate();
					} else {
						element.deactivate();
					}
					setInput(RepositoryConfig.loadAll(Database.get()));
				}

			});
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return new RepositoryConfigLabel();
		}

		@Override
		protected String[] getColumnHeaders() {
			return COLUMNS;
		}
	}

	private static class RepositoryConfigLabel extends LabelProvider
			implements ITableLabelProvider, ITableFontProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 3)
				return null;
			RepositoryConfig config = (RepositoryConfig) element;
			if (config.isActive())
				return Icon.CHECK_TRUE.get();
			return Icon.CHECK_FALSE.get();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			RepositoryConfig config = (RepositoryConfig) element;
			switch (columnIndex) {
			case 0:
				return config.getServerUrl();
			case 1:
				return config.repositoryId;
			case 2:
				return config.credentials.username;
			default:
				return null;
			}
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			RepositoryConfig config = (RepositoryConfig) element;
			if (config.isActive())
				return UI.boldFont();
			return null;
		}

	}

}
