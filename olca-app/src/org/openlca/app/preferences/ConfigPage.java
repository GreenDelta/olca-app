package org.openlca.app.preferences;

import java.util.Objects;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
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
	private Combo themeCombo;
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
		Composite body = UI.composite(parent);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true);
		Composite comp = UI.composite(body);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);
		createLanguageCombo(comp);
		if (OS.get() == OS.WINDOWS)
			createThemeCombo(comp);
		createMemoryText(comp);
		createShowHidePage(comp);

		UI.filler(comp);
		Composite bcomp = UI.composite(comp);
		UI.gridLayout(bcomp, 1, 5, 0);

		createResetWindow(bcomp);
		if (!NativeLib.isLoaded(Module.UMFPACK))
			createNativeLib(bcomp);

		UI.filler(comp);

		createNoteComposite(comp.getFont(), comp, M.Note
			+ ": ", M.SelectLanguageNoteMessage);

		return body;
	}

	private void createNativeLib(Composite comp) {
		var libButton = new Button(comp, SWT.NONE);
		libButton.setText("Download additional calculation libraries");
		Controls.onSelect(
				libButton,
				_e -> LibraryDownload.open());
		UI.gridData(libButton, true, false);
	}

	private void createResetWindow(Composite comp) {
		// reset window layout
		Button b = new Button(comp, SWT.NONE);
		b.setText("Reset window layout");
		Controls.onSelect(b, _e -> {
			WindowLayout.reset();
			b.setEnabled(false);
		});
		UI.gridData(b, true, false);
	}

	private void createShowHidePage(Composite comp) {
		// show / hide start page
		var hideStartLabel = new Label(comp, SWT.NONE);
		var gd = UI.gridData(hideStartLabel, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		hideStartLabel.setText("Hide welcome page");

		var hideStart = new Button(comp, SWT.CHECK);
		hideStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		hideStart.setSelection(
				Preferences.getBool("hide.welcome.page"));
		Controls.onSelect(hideStart, e -> Preferences.set(
				"hide.welcome.page", hideStart.getSelection()));
	}

	private void createBrowserCheck(Composite comp) {
		// Edge browser check
		var edgeLabel = new Label(comp, SWT.NONE);
		edgeLabel.setText("Use Edge as internal browser");
		var gd = UI.gridData(edgeLabel, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		var useEdge = new Button(comp, SWT.CHECK);
		useEdge.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		useEdge.setToolTipText("WebView2 needs to be installed for this");
		useEdge.setSelection(iniFile.useEdgeBrowser());
		Controls.onSelect(
				useEdge, $ -> {
					iniFile.setUseEdgeBrowser(useEdge.getSelection());
					setDirty();
				});
	}

	private void createMemoryText(Composite comp) {
		var memoryLabel = UI.label(comp, M.MaximumMemoryUsage);
		var gd = UI.gridData(memoryLabel, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		memoryText = new Text(comp, SWT.BORDER);
		UI.fillHorizontal(memoryText);
		memoryText.setText(Integer.toString(iniFile.getMaxMemory()));
		memoryText.addModifyListener(e -> setDirty());
	}

	private void createLanguageCombo(Composite composite) {
		var label = new Label(composite, SWT.NONE);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		label.setText(M.Language);

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

	private void createThemeCombo(Composite composite) {
		var label = new Label(composite, SWT.NONE);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		label.setText(M.Theme);

		themeCombo = new Combo(composite, SWT.READ_ONLY);
		UI.gridData(themeCombo, true, false);
		Theme[] themes = Theme.values();
		String[] items = new String[themes.length];
		for (int i = 0; i < themes.length; i++) {
			items[i] = themes[i].getName();
		}
		themeCombo.setItems(items);
		selectTheme(iniFile.getTheme());
		Controls.onSelect(themeCombo, (e) -> {
			int idx = themeCombo.getSelectionIndex();
			if (idx < 0)
				return;
			Theme theme = Theme.values()[idx];
			if (!Objects.equals(theme, iniFile.getTheme())) {
				iniFile.setTheme(theme);
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

	private void selectTheme(Theme theme) {
		if (theme == null)
			return;
		String[] items = themeCombo.getItems();
		int item = -1;
		for (int i = 0; i < items.length; i++) {
			if (Objects.equals(theme.getName(), items[i])) {
				item = i;
				break;
			}
		}
		if (item != -1)
			themeCombo.select(item);
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
