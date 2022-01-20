package org.openlca.app.collaboration.ui.preferences;

import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.field.BooleanModifier;
import org.openlca.app.viewers.tables.modify.field.PasswordModifier;
import org.openlca.app.viewers.tables.modify.field.StringModifier;

public class CollaborationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "preferencepages.collaboration";
	private List<CollaborationConfiguration> configs;
	private Button enableCheckBox;
	private Button libraryCheckBox;
	private Button referenceCheckBox;
	private Button commentCheckBox;
	private ConfigurationViewer configViewer;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CollaborationPreference.getStore());
		configs = CollaborationConfigurations.get();
	}

	@Override
	protected Control createContents(Composite parent) {
		var body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		var general = new Composite(body, SWT.NONE);
		UI.gridLayout(general, 2, 0, 0);
		createEnableCheckBox(general);
		createLibraryCheckBox(general);
		createReferenceCheckBox(general);
		createCommentCheckBox(general);
		UI.formLabel(body, M.ServerConfigurations);
		configViewer = new ConfigurationViewer(body);
		configViewer.setInput(configs);
		checkEnabled();
		return body;
	}

	private void createEnableCheckBox(Composite parent) {
		enableCheckBox = UI.formCheckBox(parent, M.EnableCollaboration);
		UI.gridData(enableCheckBox, true, false).horizontalIndent = 5;
		Controls.onSelect(enableCheckBox, (e) -> checkEnabled());
		enableCheckBox.setSelection(CollaborationPreference.enabled());
	}

	private void createLibraryCheckBox(Composite parent) {
		libraryCheckBox = UI.formCheckBox(parent, M.CheckAgainstLibraries);
		UI.gridData(libraryCheckBox, true, false).horizontalIndent = 5;
		libraryCheckBox.setSelection(CollaborationPreference.checkAgainstLibraries());
	}

	private void createReferenceCheckBox(Composite parent) {
		referenceCheckBox = UI.formCheckBox(parent, "Check referenced changes");
		UI.gridData(referenceCheckBox, true, false).horizontalIndent = 5;
		referenceCheckBox.setSelection(CollaborationPreference.checkReferences());
	}

	private void createCommentCheckBox(Composite parent) {
		commentCheckBox = UI.formCheckBox(parent, M.EnableComments);
		UI.gridData(commentCheckBox, true, false).horizontalIndent = 5;
		commentCheckBox.setSelection(CollaborationPreference.commentsEnabled());
	}

	private void checkEnabled() {
		libraryCheckBox.setEnabled(enableCheckBox.getSelection());
		referenceCheckBox.setEnabled(enableCheckBox.getSelection());
		commentCheckBox.setEnabled(enableCheckBox.getSelection());
		configViewer.setEnabled(enableCheckBox.getSelection());
	}

	@Override
	public boolean performOk() {
		var store = CollaborationPreference.getStore();
		store.setValue(CollaborationPreference.ENABLE, enableCheckBox.getSelection());
		store.setValue(CollaborationPreference.CHECK_AGAINST_LIBRARIES, libraryCheckBox.getSelection());
		store.setValue(CollaborationPreference.CHECK_REFERENCES, referenceCheckBox.getSelection());
		store.setValue(CollaborationPreference.DISPLAY_COMMENTS, commentCheckBox.getSelection());
		CollaborationConfigurations.save(configs);
		return true;
	}

	private static final String URL = M.ServerUrl;
	private static final String USER = M.User;
	private static final String PASS = M.Password;
	private static final String DEFAULT = M.IsDefault;

	private class ConfigurationViewer extends AbstractTableViewer<CollaborationConfiguration> {

		protected ConfigurationViewer(Composite parent) {
			super(parent);
			getModifySupport().bind(URL, new StringModifier<>("url"));
			getModifySupport().bind(USER, new StringModifier<>("user"));
			getModifySupport().bind(PASS, new PasswordModifier<>("password"));
			getModifySupport().bind(DEFAULT, new BooleanModifier<>("isDefault", (conf) -> {
				for (var config : configs) {
					if (!conf.equals(config)) {
						config.isDefault = false;
					}
				}
				getViewer().refresh(true);
			}));
			Tables.bindColumnWidths(getViewer(), 0.4, 0.2, 0.2, 0.2);
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return new LabelProvider();
		}

		@Override
		protected String[] getColumnHeaders() {
			return new String[] { URL, USER, PASS, DEFAULT };
		}

		@OnAdd
		public void onAdd() {
			var newConfig = new CollaborationConfiguration();
			newConfig.url = "newServer";
			configs.add(newConfig);
			setInput(configs);
		}

		@OnRemove
		public void onRemove() {
			var selected = Viewers.getAllSelected(getViewer());
			selected.forEach(config -> configs.remove(config));
			setInput(configs);
		}

	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int column) {
			var config = (CollaborationConfiguration) element;
			switch (column) {
			case 0:
				return config.url;
			case 1:
				return config.user;
			case 2:
				var echoChar = Character.toString((char) 8226);
				return config.password.replaceAll(".", echoChar);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column != 3)
				return null;
			var config = (CollaborationConfiguration) element;
			return Images.get(config.isDefault);
		}

	}

}
