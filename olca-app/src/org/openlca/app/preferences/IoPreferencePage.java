package org.openlca.app.preferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.openlca.app.M;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.ilcd.descriptors.DataStock;
import org.openlca.ilcd.io.AuthInfo;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.ilcd.io.SodaConnection;
import org.openlca.util.Strings;

public class IoPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String ID = "org.openlca.io.IoPreferencePage";
	private final List<FieldEditor> editors = new ArrayList<>();

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = UI.composite(parent);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, true);
		createIlcdNetworkContents(comp);
		UI.gridData(new Label(parent,
						SWT.SEPARATOR | SWT.HORIZONTAL),
				true, false);
		createIlcdOtherContents(comp);
		return comp;
	}

	private void createIlcdNetworkContents(Composite parent) {
		Group section = UI.group(parent, SWT.SHADOW_OUT);
		section.setText(M.ILCDNetworkSettings);
		StringFieldEditor urlEditor = new StringFieldEditor(
				IoPreference.ILCD_URL, M.URL, section);
		addField(urlEditor);
		StringFieldEditor userEditor = new StringFieldEditor(
				IoPreference.ILCD_USER, M.User, section);
		addField(userEditor);
		StringFieldEditor passwordEditor = new StringFieldEditor(
				IoPreference.ILCD_PASSWORD, M.Password, section);
		passwordEditor.getTextControl(section).setEchoChar('*');
		addField(passwordEditor);
		UI.gridLayout(section, 2);
		UI.gridData(section, true, false);
	}

	private void createIlcdOtherContents(Composite parent) {
		Group section = new Group(parent, SWT.SHADOW_OUT);
		section.setText(M.ILCDOtherSettings);
		ComboFieldEditor langEditor = new ComboFieldEditor(
				IoPreference.ILCD_LANG, M.Language, getLanguages(),
				section);
		addField(langEditor);
		UI.gridLayout(section, 2);
		UI.gridData(section, true, false);
	}

	private String[][] getLanguages() {
		var displayLang = new Locale(Language.getApplicationLanguage().getCode());
		List<String[]> namesAndValues = new ArrayList<>();
		Set<String> added = new HashSet<>();
		for (var locale : Locale.getAvailableLocales()) {
			String lang = locale.getLanguage();
			if (added.contains(lang))
				continue;
			String[] nameAndValue = new String[2];
			nameAndValue[0] = locale.getDisplayLanguage(displayLang);
			nameAndValue[1] = lang;
			namesAndValues.add(nameAndValue);
			added.add(lang);
		}
		namesAndValues.sort((o1, o2) -> Strings.compare(o1[0], o2[0]));
		return namesAndValues.toArray(new String[namesAndValues.size()][]);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setText(M.Test);
		setImageDescriptor(RcpActivator.imageDescriptorFromPlugin(
				RcpActivator.PLUGIN_ID, "icons/network16.png"));
	}

	@Override
	protected void performApply() {
		super.performApply();
		var con = new SodaConnection();
		con.url = IoPreference.getIlcdUrl();
		con.user = IoPreference.getIlcdUser();
		con.password = IoPreference.getIlcdPassword();
		try (var client = SodaClient.of(con)) {
			testConnection(client);
		}
	}

	@Override
	public boolean performOk() {
		for (FieldEditor editor : editors)
			editor.store();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		IoPreference.reset();
		for (FieldEditor editor : editors)
			editor.load();
	}

	private void testConnection(SodaClient client) {
		try {
			AuthInfo info = client.getAuthInfo();
			if (!info.isAuthenticated()) {
				MsgBox.info(M.ConnectionWithAnonymousAccess);
				return;
			}
			DataStock stock = info.getDataStocks().isEmpty()
					? null
					: info.getDataStocks().get(0);
			if (stock == null
					|| !stock.isReadAllowed()
					|| !stock.isExportAllowed()) {
				MsgBox.warning(M.YouDoNotHaveReadOrWriteAccess);
				return;
			}
			MsgBox.info(M.ConnectionWorks
					+ " (" + M.DataStock + " = " + stock.getShortName() + ")");
		} catch (Exception e) {
			MsgBox.error(M.ILCDConnectionFailedErr + " - " + e.getMessage());
		}
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(RcpActivator.getDefault().getPreferenceStore());
	}

	public static void open(Shell shell) {
		PreferencesUtil.createPreferenceDialogOn(shell, IoPreferencePage.ID,
				null, null).open();
	}

	private void addField(FieldEditor editor) {
		editors.add(editor);
		editor.setPage(this);
		editor.setPreferenceStore(getPreferenceStore());
		editor.load();
	}
}
