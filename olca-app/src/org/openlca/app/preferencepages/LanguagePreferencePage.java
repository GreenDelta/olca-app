package org.openlca.app.preferencepages;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.ApplicationProperties;
import org.openlca.app.Messages;
import org.openlca.app.util.Question;

public class LanguagePreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Combo combo;
	private int defaultLanguage;
	private boolean isDirty = false;
	private Language language;

	/**
	 * Default constructor
	 */
	public LanguagePreferencePage() {
	}

	/**
	 * Initializes the listeners
	 */
	private void initListeners() {
		combo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no default selection action
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				// actual language
				final Language old = language;
				// get selection
				language = Language.values()[combo.getSelectionIndex()];
				if (old != language) {
					// selection changed
					getApplyButton().setEnabled(true);
					isDirty = true;
				}
			}

		});
	}

	@Override
	protected Control createContents(final Composite parent) {
		// create body
		final Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// create composite for selection a language
		final Composite composite = new Composite(body, SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		final Label message = new Label(composite, SWT.NONE);
		message.setText(Messages.SelectAUserInterfaceLanguage);

		// create language selection combo
		combo = new Combo(composite, SWT.READ_ONLY);
		final String[] languages = new String[Language.values().length];
		int selected = 0;
		// collect display names and initial selection
		for (int i = 0; i < Language.values().length; i++) {
			languages[i] = Language.values()[i].getDisplayName();
			if (Language.values()[i] == language) {
				selected = i;
			}
			if (Language.values()[i] == Language
					.getLanguage(ApplicationProperties.PROP_NATIONAL_LANGUAGE
							.getDefaultValue())) {
				defaultLanguage = i;
			}
		}
		combo.setItems(languages);
		combo.select(selected);

		// create composite for displaying the note
		final Composite composite2 = new Composite(body, SWT.NONE);
		composite2.setLayout(new GridLayout(1, true));
		createNoteComposite(composite2.getFont(), composite2, Messages.Note
				+ ":", Messages.SelectLanguageNoteMessage);

		initListeners();
		return body;
	}

	@Override
	protected void performApply() {
		ApplicationProperties.PROP_NATIONAL_LANGUAGE.setValue(language
				.getCode());
		getApplyButton().setEnabled(false);
		isDirty = false;
	}

	@Override
	protected void performDefaults() {
		combo.select(defaultLanguage);
		language = Language
				.getLanguage(ApplicationProperties.PROP_NATIONAL_LANGUAGE
						.getDefaultValue());
		super.performDefaults();
		performApply();
	}

	@Override
	public void createControl(final Composite parent) {
		super.createControl(parent);
		getApplyButton().setEnabled(false);
	}

	@Override
	public String getTitle() {
		return Messages.Language;
	}

	@Override
	public void init(final IWorkbench workbench) {
		// get actual national language
		language = Language
				.getLanguage(ApplicationProperties.PROP_NATIONAL_LANGUAGE
						.getValue());
	}

	@Override
	public boolean performOk() {
		if (isDirty) {
			if (Question.ask(Messages.SaveChangesQuestion,
					Messages.SaveChangesQuestion)) {
				performApply();
			}
		}
		return true;
	}

}
