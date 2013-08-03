package org.openlca.app.preferencepages;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.ApplicationProperties;
import org.openlca.app.Messages;
import org.openlca.app.util.UIFactory;

public class MemoryPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * The actual amount of reserved memory
	 */
	private int amount;

	/**
	 * Text to enter a new memory amount
	 */
	private Text memoryText;

	/**
	 * Initializes the listeners of the widgets
	 */
	private void initListeners() {
		// listen on modification of the text widget
		memoryText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				try {
					final int newAmount = Integer.parseInt(memoryText.getText());
					amount = newAmount;
				} catch (final NumberFormatException ex) {
					memoryText.setText(Integer.toString(amount));
				}
			}
		});
	}

	@Override
	protected Control createContents(final Composite parent) {
		// create body
		final Composite body = UIFactory.createContainer(parent,
				UIFactory.createGridLayout(2, false, 5));

		// create the text to enter a new memory amount
		memoryText = UIFactory.createTextWithLabel(body, Messages.Reserve
				+ " (MB):", false);
		memoryText.setText(Integer.toString(amount));

		// create composite for displaying the note
		final Composite composite2 = new Composite(body, SWT.NONE);
		composite2.setLayout(new GridLayout(1, true));
		createNoteComposite(composite2.getFont(), composite2, Messages.Note
				+ ":", Messages.LanguagePreferencePage_SelectLanguageNoteText);

		// initialize the listeners
		initListeners();

		return body;
	}

	@Override
	protected void performApply() {
		if (amount < 256) {
			amount = 256;
			memoryText.setText("256");
		}
		ApplicationProperties.PROP_MEMORY.setValue(Integer.toString(amount));
	}

	@Override
	protected void performDefaults() {
		memoryText.setText(ApplicationProperties.PROP_MEMORY.getDefaultValue());
	}

	@Override
	public void init(final IWorkbench workbench) {
		amount = Integer.parseInt(ApplicationProperties.PROP_MEMORY.getValue());
	}

}
