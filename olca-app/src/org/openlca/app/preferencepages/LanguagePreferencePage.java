package org.openlca.app.preferencepages;

import java.util.Objects;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.Messages;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguagePreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private boolean isDirty = false;
	private Combo combo;
	private IniFile iniFile;

	@Override
	public String getTitle() {
		return Messages.Language;
	}

	@Override
	public void init(IWorkbench workbench) {
		try {
			iniFile = IniFile.read();
		} catch (Exception e) {
			log.error("failed to read openLCA.ini", e);
			iniFile = new IniFile();
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
		Label message = new Label(composite, SWT.NONE);
		message.setText(Messages.Language);
		combo = new Combo(composite, SWT.READ_ONLY);
		UI.gridData(combo, true, false);
		initComboValues();
		new Label(composite, SWT.NONE);
		createNoteComposite(composite.getFont(), composite, Messages.Note
				+ ": ", Messages.SelectLanguageNoteMessage);
		initListener();
		return body;
	}

	private void initComboValues() {
		Language[] languages = Language.values();
		String[] items = new String[languages.length];
		int selectedItem = -1;
		for (int i = 0; i < languages.length; i++) {
			items[i] = languages[i].getDisplayName();
			if (Objects.equals(languages[i], iniFile.getLanguage()))
				selectedItem = i;
		}
		combo.setItems(items);
		if (selectedItem != -1)
			combo.select(selectedItem);
	}

	private void initListener() {
		combo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = combo.getSelectionIndex();
				if (idx < 0)
					return;
				Language language = Language.values()[idx];
				if (!Objects.equals(language, iniFile.getLanguage())) {
					getApplyButton().setEnabled(true);
					iniFile.setLanguage(language);
					isDirty = true;
				}
			}
		});
	}

	@Override
	protected void performApply() {
		iniFile.write();
		getApplyButton().setEnabled(false);
		isDirty = false;
	}

	@Override
	protected void performDefaults() {
		Language defaultLang = Language.ENGLISH;
		if (Objects.equals(iniFile.getLanguage(), defaultLang))
			return;
		String[] items = combo.getItems();
		int item = -1;
		for (int i = 0; i < items.length; i++) {
			if (Objects.equals(defaultLang.getDisplayName(), items[i])) {
				item = i;
				break;
			}
		}
		if (item == -1)
			return;
		combo.select(item);
		iniFile.setLanguage(defaultLang);
		super.performDefaults();
		performApply();
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
