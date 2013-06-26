/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.flow;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.ui.Labels;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.viewer.ISelectionChangedListener;
import org.openlca.ui.viewer.LocationViewer;

/**
 * Form page for displaying and editing a flow object
 * 
 * @author Sebastian Greve
 * 
 */
public class FlowInfoPage extends ModelEditorInfoPage implements
		PropertyChangeListener {

	private Text casNumberText;
	private Flow flow;
	private Text flowTypeText;
	private Text formulaText;
	private LocationViewer locationViewer;
	private IMessageManager messageManager;

	public FlowInfoPage(ModelEditor editor) {
		super(editor, "FlowInfoPage", Messages.Common_GeneralInformation, //$NON-NLS-1$
				Messages.Common_GeneralInformation);
		this.flow = (Flow) editor.getModelComponent();
	}

	/**
	 * Checks if the given cas number string is valid
	 * 
	 * @param cas
	 *            the cas number to be proven
	 * @return true if the cas number is valid, false otherwise
	 */
	private String checkCAS(final String cas) {
		boolean error = true;
		if (cas != null && cas.length() >= 7) {
			// check right syntax
			final char[] chars = cas.toCharArray();
			if (chars[chars.length - 1] >= 48 && chars[chars.length - 1] <= 57) {
				if (chars[chars.length - 2] == '-') {
					if (chars[chars.length - 3] >= 48
							&& chars[chars.length - 3] <= 57
							&& chars[chars.length - 4] >= 48
							&& chars[chars.length - 4] <= 57) {
						if (chars[chars.length - 5] == '-') {
							error = false;
							int i = 0;
							while (!error && i < chars.length - 5) {
								if (chars[chars.length - 1] < 48
										&& chars[chars.length - 1] > 57) {
									error = true;
								} else {
									i++;
								}
							}
							if (!error) {
								// validate checksum
								final int[] numbers = new int[chars.length - 3];
								for (int j = 0; j < chars.length - 5; j++) {
									numbers[j] = Integer
											.parseInt(chars[j] + ""); //$NON-NLS-1$
								}
								numbers[numbers.length - 2] = Integer
										.parseInt(chars[chars.length - 4] + ""); //$NON-NLS-1$
								numbers[numbers.length - 1] = Integer
										.parseInt(chars[chars.length - 3] + ""); //$NON-NLS-1$
								int sum = 0;
								for (int j = 0; j < numbers.length; j++) {
									sum += (numbers.length - j) * numbers[j];
								}
								if (sum % 10 != Integer.parseInt("" //$NON-NLS-1$
										+ chars[chars.length - 1])) {
									error = true;
								}
							}
						}
					}
				}
			}
		}
		return error ? Messages.Flows_FlowInfoPage_CASNotValid : null;
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		super.createContents(body, toolkit);

		if (flow.getFlowType() != FlowType.ELEMENTARY_FLOW) {
			FlowUseSection useSection = new FlowUseSection(flow, Database.get());
			useSection.render(body, toolkit);
		}

		messageManager = getForm().getMessageManager();

		flowTypeText = UIFactory.createTextWithLabel(getMainComposite(),
				toolkit, "", false);
		flowTypeText.setEditable(false);

		final Section flowInfoSection = UIFactory.createSection(body, toolkit,
				Messages.Common_AdditionalInfo, true, false);

		final Composite flowInfoComposite = UIFactory.createSectionComposite(
				flowInfoSection, toolkit, UIFactory.createGridLayout(2));

		casNumberText = UIFactory.createTextWithLabel(flowInfoComposite,
				toolkit, Messages.Flows_CasNumber, false);

		formulaText = UIFactory.createTextWithLabel(flowInfoComposite, toolkit,
				Messages.Common_FormulaTitle, false);

		final Section locationInfoSection = UIFactory.createSection(body,
				toolkit, Messages.Flows_LocationInfoSectionLabel, true, false);

		final Composite locationInfoComposite = UIFactory
				.createSectionComposite(locationInfoSection, toolkit,
						UIFactory.createGridLayout(2));

		UI.formLabel(locationInfoComposite, Messages.Location);
		locationViewer = new LocationViewer(locationInfoComposite);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Common_Flow + ": " //$NON-NLS-1$
				+ (flow != null ? flow.getName() != null ? flow.getName() : "" //$NON-NLS-1$
				: ""); //$NON-NLS-1$
		return title;
	}

	@Override
	protected void initListeners() {
		super.initListeners();

		casNumberText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				final String error = checkCAS(casNumberText.getText());
				if (error == null) {
					flow.setCasNumber(casNumberText.getText());
					messageManager.removeMessage("cas"); //$NON-NLS-1$
				} else {
					messageManager.addMessage("cas", error, casNumberText, //$NON-NLS-1$
							IMessageProvider.ERROR);
				}
			}

		});

		formulaText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				flow.setFormula(formulaText.getText());
			}

		});

		locationViewer
				.addSelectionChangedListener(new ISelectionChangedListener<Location>() {

					@Override
					public void selectionChanged(Location selection) {
						flow.setLocation(selection);
					}
				});
	}

	@Override
	protected void setData() {
		super.setData();
		flowTypeText.setText(Labels.flowType(flow));
		locationViewer.setInput(Database.get());
		if (flow != null) {

			if (flow.getCasNumber() != null) {
				casNumberText.setText(flow.getCasNumber());
			}

			if (flow.getFormula() != null) {
				formulaText.setText(flow.getFormula());
			}
			locationViewer.select(flow.getLocation());
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("referenceFlowProperty")) { //$NON-NLS-1$

		}
	}

}
