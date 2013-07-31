/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.db;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.util.UIFactory;

/**
 * Wizard page for editing the properties of a data provider
 * 
 * @author Sebastian Greve
 * 
 */
public class MySQLPropertiesPage extends WizardPage {

	/**
	 * Indicates if the MySQL is embedded
	 */
	private boolean embedded = true;

	/**
	 * The host
	 */
	private String host = "localhost";

	/**
	 * The password
	 */
	private String password;

	/**
	 * The port to use
	 */
	private String port = "3306";

	/**
	 * The MySQL user
	 */
	private String user = "root";

	/**
	 * Creates a new instance
	 */
	protected MySQLPropertiesPage() {
		super("MySQLPropertiesPage");
		setPageComplete(false);
		setTitle(Messages.MySQLWizard_Title);
		setDescription(Messages.MySQLWizard_Description);
	}

	/**
	 * Checks if the required properties are set
	 * 
	 * @return True if all required properties are set, false otherwise
	 */
	private boolean isComplete() {
		boolean complete = host != null && port != null && user != null;
		try {
			Integer.parseInt(port);
			setErrorMessage(null);
		} catch (final Exception e) {
			complete = false;
			setErrorMessage(Messages.PortError);
		}
		return complete;
	}

	@Override
	public void createControl(final Composite parent) {
		// create body
		final Composite composite = UIFactory.createContainer(parent);

		// create host text
		final Text hostText = UIFactory.createTextWithLabel(composite,
				Messages.Host, false);
		hostText.setText("localhost");
		hostText.setEnabled(false);

		// create port text
		final Text portText = UIFactory.createTextWithLabel(composite,
				Messages.Port, false);
		portText.setText("3306");

		// create user text
		final Text userText = UIFactory.createTextWithLabel(composite,
				Messages.Username, false);
		userText.setText("root");

		// create password text
		final Text passText = UIFactory.createTextWithLabel(composite,
				Messages.Password, false, SWT.PASSWORD);

		// create embedded button
		final Button embeddedButton = UIFactory.createButton(composite,
				Messages.Embedded);
		embeddedButton.setSelection(true);

		// initialize listeners
		hostText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				host = hostText.getText();
				if (host.equals("")) {
					host = null;
				}
				setPageComplete(isComplete());
			}
		});

		portText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				port = portText.getText();
				if (port.equals("")) {
					port = null;
				}
				setPageComplete(isComplete());
			}
		});

		userText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				user = userText.getText();
				if (user.equals("")) {
					user = null;
				}
				setPageComplete(isComplete());
			}
		});

		passText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				password = passText.getText();
				if (password.equals("")) {
					password = null;
				}
				setPageComplete(isComplete());
			}
		});

		embeddedButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				embedded = embeddedButton.getSelection();
				hostText.setEnabled(!embedded);
				setPageComplete(isComplete());
			}
		});

		setPageComplete(isComplete());
		setControl(composite);
	}

	/**
	 * Getter of the host
	 * 
	 * @return The specified host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Getter of the password
	 * 
	 * @return The specified password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Getter of the port
	 * 
	 * @return The specified port to use
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Getter of the user
	 * 
	 * @return The specified MySQL user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Getter of the embedded state
	 * 
	 * @return True if embedded was checked, false otherwise
	 */
	public boolean isEmbedded() {
		return embedded;
	}
}
