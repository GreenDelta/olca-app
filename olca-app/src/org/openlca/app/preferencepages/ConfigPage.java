package org.openlca.app.preferencepages;

import java.util.Objects;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.Config;
import org.openlca.app.Messages;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private boolean isDirty = false;
	private Combo languageCombo;
	private Text memoryText;
	private ConfigIniFile iniFile;
	private boolean browserEnabled;

	private Button browserCheck;

	@Override
	public String getTitle() {
		return Messages.Configuration;
	}

	@Override
	public void init(IWorkbench workbench) {
		try {
			iniFile = ConfigIniFile.read();
			browserEnabled = Config.isBrowserEnabled();
		} catch (Exception e) {
			log.error("failed to read openLCA.ini", e);
			iniFile = new ConfigIniFile();
		}
	}

	@Override
	protected Control createContents(final Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true);
		Composite composite = UI.formComposite(body);
		UI.gridLayout(composite, 2);
		UI.gridData(composite, true, false);
		createLanguageCombo(composite);
		memoryText = UI.formText(composite, Messages.MaximumMemoryUsage);
		memoryText.setText(Integer.toString(iniFile.getMaxMemory()));
		memoryText.addModifyListener((e) -> setDirty());
		browserCheck = UI.formCheckBox(composite, Messages.UseBrowserFeatures);
		browserCheck.setSelection(browserEnabled);
		Controls.onSelect(browserCheck, (e) -> {
			browserEnabled = browserCheck.getSelection();
			setDirty();
		});
		new Label(composite, SWT.NONE);
		createNoteComposite(composite.getFont(), composite, Messages.Note
				+ ": ", Messages.SelectLanguageNoteMessage);
		return body;
	}

	private void createLanguageCombo(Composite composite) {
		UI.formLabel(composite, Messages.Language);
		languageCombo = new Combo(composite, SWT.READ_ONLY);
		UI.gridData(languageCombo, true, false);
		initComboValues();
		Controls.onSelect(languageCombo, (e) -> {
			int idx = languageCombo.getSelectionIndex();
			if (idx < 0)
				return;
			Language language = Language.values()[idx];
			if (!Objects.equals(language, iniFile.getLanguage())) {
				iniFile.setLanguage(language);
				setDirty();
			}
		});
	}

	private void initComboValues() {
		Language[] languages = Language.values();
		String[] items = new String[languages.length];
		for (int i = 0; i < languages.length; i++) {
			items[i] = languages[i].getDisplayName();
		}
		languageCombo.setItems(items);
		selectLanguage(iniFile.getLanguage());
	}

	private void setDirty() {
		getApplyButton().setEnabled(true);
		isDirty = true;
	}

	@Override
	protected void performApply() {
		int memVal = ConfigMemCheck.parseAndCheck(memoryText.getText());
		if (memVal < 0)
			return;
		iniFile.setMaxMemory(memVal);
		iniFile.write();
		if (browserEnabled != Config.isBrowserEnabled())
			Config.setBrowserEnabled(browserEnabled);
		getApplyButton().setEnabled(false);
		isDirty = false;
	}

	@Override
	protected void performDefaults() {
		Language defaultLang = Language.ENGLISH;
		int maxMem = ConfigMemCheck.getDefault();
		browserEnabled = true;
		selectLanguage(defaultLang);
		memoryText.setText(Integer.toString(maxMem));
		browserCheck.setSelection(browserEnabled);
		iniFile.setLanguage(defaultLang);
		iniFile.setMaxMemory(maxMem);
		Config.setBrowserEnabled(browserEnabled);
		super.performDefaults();
		performApply();
	}

	private void selectLanguage(Language language) {
		if (language == null)
			return;
		String[] items = languageCombo.getItems();
		int item = -1;
		for (int i = 0; i < items.length; i++) {
			if (Objects.equals(language.getDisplayName(), items[i])) {
				item = i;
				break;
			}
		}
		if (item != -1)
			languageCombo.select(item);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setEnabled(false);
	}

	@Override
	public boolean performOk() {
		if (!isDirty)
			return true;
		if (Question.ask(Messages.SaveChangesQuestion,
				Messages.SaveChangesQuestion)) {
			performApply();
		}
		return true;
	}
}
