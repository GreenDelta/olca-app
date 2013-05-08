/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.navigator;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.ui.UI;

/**
 * Window displaying the properties of a data provider
 * 
 * @author Sebastian Greve
 * 
 */
public class DataProviderPropertiesWindow extends FormDialog {

	/**
	 * The database provider
	 */
	private final IDatabaseServer dataProvider;

	/**
	 * Creates a new instance
	 * 
	 * @param dataProvider
	 *            The data provider
	 */
	public DataProviderPropertiesWindow(final IDatabaseServer dataProvider) {
		super(UI.shell());
		this.dataProvider = dataProvider;
	}

	@Override
	protected void createFormContent(final IManagedForm managedForm) {
		// configure form
		managedForm.getForm().setText(Messages.Properties);
		managedForm.getToolkit().getHyperlinkGroup()
				.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		managedForm.getToolkit().decorateFormHeading(
				managedForm.getForm().getForm());

		// create body
		final Composite body = managedForm.getForm().getBody();
		body.setLayout(new GridLayout(2, false));
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		managedForm.getToolkit().adapt(body);

		// get data provider properties
		final Map<String, String> properties = dataProvider.getProperties();

		// for each property
		for (final Entry<String, String> property : properties.entrySet()) {
			if (!property.getKey().equals("id")
					&& !property.getKey().equals("connectAtStartup")) {
				// create label
				final Label label = new Label(body, SWT.NONE);
				label.setText(property.getKey());
				label.setBackground(body.getBackground());

				// create text field
				final Text text = new Text(body, SWT.BORDER);
				text.setText(property.getValue());
				text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				text.setEditable(false);
			}
		}

		final Label label = new Label(body, SWT.NONE);
		label.setBackground(body.getBackground());

		managedForm.getToolkit().paintBordersFor(body);
	}
}
