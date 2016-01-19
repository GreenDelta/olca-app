package org.openlca.app.preferencepages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.openlca.app.Messages;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.UI;
import org.openlca.ilcd.io.Authentication;
import org.openlca.ilcd.io.NetworkClient;
import org.openlca.util.Strings;

public class IoPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String ID = "org.openlca.io.IoPreferencePage";
	private final List<FieldEditor> editors = new ArrayList<>();

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, true);
		createIlcdNetworkContents(UI.formComposite(comp));
		UI.horizontalSeparator(comp);
		createIlcdOtherContents(UI.formComposite(comp));
		return comp;
	}

	private void createIlcdNetworkContents(Composite parent) {
		StringFieldEditor urlEditor = new StringFieldEditor(
				IoPreference.ILCD_URL, Messages.URL, parent);
		addField(urlEditor);
		StringFieldEditor userEditor = new StringFieldEditor(
				IoPreference.ILCD_USER, Messages.User, parent);
		addField(userEditor);
		StringFieldEditor passwordEditor = new StringFieldEditor(
				IoPreference.ILCD_PASSWORD, Messages.Password, parent);
		passwordEditor.getTextControl(parent).setEchoChar('*');
		addField(passwordEditor);
	}

	private void createIlcdOtherContents(Composite parent) {
		ComboFieldEditor langEditor = new ComboFieldEditor(
				IoPreference.ILCD_LANG, Messages.Language, getLanguages(),
				parent);
		addField(langEditor);
	}

	private String[][] getLanguages() {
		Locale displayLang = new Locale(Language.getApplicationLanguage()
				.getCode());
		Locale[] all = Locale.getAvailableLocales();
		List<String[]> namesAndValues = new ArrayList<>();
		Set<String> added = new HashSet<>();
		for (int i = 0; i < all.length; i++) {
			String lang = all[i].getLanguage();
			if (added.contains(lang))
				continue;
			String[] nameAndValue = new String[2];
			nameAndValue[0] = all[i].getDisplayLanguage(displayLang);
			nameAndValue[1] = lang;
			namesAndValues.add(nameAndValue);
			added.add(lang);
		}
		Collections.sort(namesAndValues, new Comparator<String[]>() {
			@Override
			public int compare(String[] o1, String[] o2) {
				return Strings.compare(o1[0], o2[0]);
			}
		});
		return namesAndValues.toArray(new String[namesAndValues.size()][]);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setText(Messages.Test);
		setImageDescriptor(RcpActivator.imageDescriptorFromPlugin(
				RcpActivator.PLUGIN_ID, "icons/network16.png"));
	}

	@Override
	protected void performApply() {
		super.performApply();
		String url = IoPreference.getIlcdUrl();
		String user = IoPreference.getIlcdUser();
		String password = IoPreference.getIlcdPassword();
		NetworkClient client = new NetworkClient(url, user, password);
		testConnection(client);
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

	private void testConnection(NetworkClient client) {
		try {
			client.connect();
			checkAuthentication(client.getAuthentication());
		} catch (Exception e) {
			Dialog.showError(getShell(), Messages.ILCD_CONNECTION_FAILED_MSG
					+ " (" + e.getMessage() + ")");
		}
	}

	private void checkAuthentication(Authentication authentication) {
		if (!authentication.isAuthenticated())
			Dialog.showError(getShell(),
					Messages.ILCD_AUTHENTICATION_FAILED_MSG);
		else if (!authentication.isReadAllowed()
				|| !authentication.isExportAllowed())
			Dialog.showWarning(getShell(),
					Messages.ILCD_NO_READ_OR_WRITE_ACCESS_MSG);
		else
			Dialog.showInfo(getShell(), Messages.ILCD_CONNECTION_WORKS_MSG);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(RcpActivator.getDefault().getPreferenceStore());
	}

	public static final void open(Shell shell) {
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
