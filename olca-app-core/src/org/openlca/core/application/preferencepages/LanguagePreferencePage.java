/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.preferencepages;

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
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.Messages;
import org.openlca.ui.Question;

/**
 * The {@link PreferencePage} for the language support
 * 
 * @see IWorkbenchPreferencePage
 * 
 * @author Sebastian Greve
 * 
 */
public class LanguagePreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * The {@link Combo} to select the {@link Language}
	 */
	private Combo combo;

	/**
	 * The default {@link Language}
	 */
	private int defaultLanguage;

	/**
	 * Indicates the dirty state of this page
	 */
	private boolean isDirty = false;

	/**
	 * The selected {@link Language}
	 */
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
		message.setText(Messages.LanguagePreferencePage_SelectLanguageLabel);

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
				+ ":", Messages.LanguagePreferencePage_SelectLanguageNoteText);

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
		return Messages.LanguagePreferencePage_Title;
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
			if (Question.ask(Messages.Common_SaveChangesQuestion,
					Messages.Common_SaveChangesQuestion)) {
				performApply();
			}
		}
		return true;
	}

}
