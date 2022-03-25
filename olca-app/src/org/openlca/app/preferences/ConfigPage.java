package org.openlca.app.preferences;

import java.util.Objects;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.M;
import org.openlca.app.rcp.WindowLayout;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigPage extends PreferencePage implements
	IWorkbenchPreferencePage {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private boolean isDirty = false;
	private Combo languageCombo;
	private Text memoryText;
	private ConfigIniFile iniFile;

	@Override
	public String getTitle() {
		return M.Configuration;
	}

	@Override
	public void init(IWorkbench workbench) {
		try {
			iniFile = ConfigIniFile.read();
		} catch (Exception e) {
			log.error("failed to read openLCA.ini", e);
			iniFile = new ConfigIniFile();
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true);
		Composite comp = UI.formComposite(body);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);
		createLanguageCombo(comp);

		memoryText = UI.formText(comp, M.MaximumMemoryUsage);
		memoryText.setText(Integer.toString(iniFile.getMaxMemory()));
		memoryText.addModifyListener(e -> setDirty());

		// Edge browser check
		if (OS.get() == OS.WINDOWS) {
			var useEdge = UI.formCheckBox(
				comp, "Use Edge as internal browser");
			useEdge.setToolTipText("WebView2 needs to be installed for this");
			useEdge.setSelection(iniFile.useEdgeBrowser());
			Controls.onSelect(
				useEdge, $ -> {
					iniFile.setUseEdgeBrowser(useEdge.getSelection());
					setDirty();
				});
		}

		// show / hide start page
		var hideStart = UI.formCheckBox(
			comp, "Hide welcome page");
		hideStart.setSelection(
			Preferences.getBool("hide.welcome.page"));
		Controls.onSelect(hideStart, e -> Preferences.set(
			"hide.welcome.page", hideStart.getSelection()));

		// reset window layout
		UI.filler(comp);
		Composite bcomp = new Composite(comp, SWT.NONE);
		UI.gridLayout(bcomp, 1, 5, 0);
		Button b = new Button(bcomp, SWT.NONE);
		b.setText("Reset window layout");
		Controls.onSelect(b, _e -> {
			WindowLayout.reset();
			b.setEnabled(false);
		});

		if (!NativeLib.isLoaded(Module.UMFPACK)) {
			var libButton = new Button(bcomp, SWT.NONE);
			libButton.setText("Download additional calculation libraries");
			Controls.onSelect(
				libButton,
				_e -> LibraryDownload.open());
			UI.gridData(b, true, false);
			UI.gridData(libButton, true, false);
		}

		UI.filler(comp);
		createNoteComposite(comp.getFont(), comp, M.Note
			+ ": ", M.SelectLanguageNoteMessage);
		return body;
	}

	private void createLanguageCombo(Composite composite) {
		UI.formLabel(composite, M.Language);
		languageCombo = new Combo(composite, SWT.READ_ONLY);
		UI.gridData(languageCombo, true, false);
		Language[] languages = Language.values();
		String[] items = new String[languages.length];
		for (int i = 0; i < languages.length; i++) {
			items[i] = languages[i].getDisplayName();
		}
		languageCombo.setItems(items);
		selectLanguage(iniFile.getLanguage());
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


	private void setDirty() {
		getApplyButton().setEnabled(true);
		isDirty = true;
	}

	@Override
	protected void performApply() {
		int memVal = ConfigMemCheck.parseAndCheck(memoryText.getText());
		if (memVal < 256) {
			memVal = 256;
		}
		iniFile.setMaxMemory(memVal);
		iniFile.write();
		getApplyButton().setEnabled(false);
		isDirty = false;
	}

	@Override
	protected void performDefaults() {
		Language defaultLang = Language.ENGLISH;
		int maxMem = ConfigMemCheck.getDefault();
		selectLanguage(defaultLang);
		memoryText.setText(Integer.toString(maxMem));
		iniFile.setLanguage(defaultLang);
		iniFile.setMaxMemory(maxMem);
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
		if (item != -1) {
			languageCombo.select(item);
		}
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setEnabled(false);
	}

	@Override
	public boolean performOk() {
		if (isDirty) {
			performApply();
		}
		return true;
	}
}
