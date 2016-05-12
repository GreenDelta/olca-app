package org.openlca.app.cloud.ui.preferences;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
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
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.field.BooleanModifier;
import org.openlca.app.viewers.table.modify.field.PasswordModifier;
import org.openlca.app.viewers.table.modify.field.StringModifier;

public class CloudPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "preferencepages.cloud";
	private List<CloudConfiguration> configs;
	private Button libraryCheckBox;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CloudPreference.getStore());
		configs = CloudConfigurations.get();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		Composite general = new Composite(body, SWT.NONE);
		UI.gridLayout(general, 2, 0, 0);
		createLibraryCheckBox(general);
		UI.formLabel(body, "#Server configurations");
		ConfigurationViewer viewer = new ConfigurationViewer(body);
		viewer.setInput(configs);
		return body;
	}

	private void createLibraryCheckBox(Composite parent) {
		libraryCheckBox = UI.formCheckBox(parent, M.CheckAgainstLibraries);
		libraryCheckBox.setSelection(CloudPreference.doCheckAgainstLibraries());
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = CloudPreference.getStore();
		store.setValue(CloudPreference.CHECK_AGAINST_LIBRARIES, libraryCheckBox.getSelection());
		CloudConfigurations.save(configs);
		return true;
	}

	private static final String URL = M.ServerUrl;
	private static final String USER = M.User;
	private static final String PASS = M.Password;
	private static final String DEFAULT = "#Is default";

	private class ConfigurationViewer extends AbstractTableViewer<CloudConfiguration> {

		protected ConfigurationViewer(Composite parent) {
			super(parent);
			getModifySupport().bind(URL, new StringModifier<>("url"));
			getModifySupport().bind(USER, new StringModifier<>("user"));
			getModifySupport().bind(PASS, new PasswordModifier<>("password"));
			getModifySupport().bind(DEFAULT, new BooleanModifier<>("isDefault", (conf) -> {
				for (CloudConfiguration config : configs)
					if (!conf.equals(config))
						config.isDefault = false;
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
			CloudConfiguration newConfig = new CloudConfiguration();
			newConfig.url = "newServer";
			configs.add(newConfig);
			setInput(configs);
		}

		@OnRemove
		public void onRemove() {
			List<CloudConfiguration> selected = Viewers.getAllSelected(getViewer());
			for (CloudConfiguration config : selected)
				configs.remove(config);
			setInput(configs);
		}

	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int column) {
			CloudConfiguration config = (CloudConfiguration) element;
			switch (column) {
			case 0:
				return config.url;
			case 1:
				return config.user;
			case 2:
				String echoChar = Character.toString((char) 8226);
				return config.password.replaceAll(".", echoChar);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column != 3)
				return null;
			CloudConfiguration config = (CloudConfiguration) element;
			if (config.isDefault)
				return Icon.CHECK_TRUE.get();
			return Icon.CHECK_FALSE.get();
		}

	}

}
