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

import java.util.UUID;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.viewer.FlowPropertyViewer;
import org.openlca.ui.viewer.FlowTypeViewer;
import org.openlca.ui.viewer.ISelectionChangedListener;

/**
 * Wizard page for creating a new flow object
 * 
 * @author Sebastian Greve
 * 
 */
public class FlowWizardPage extends ModelWizardPage {

	/**
	 * A {@link Combo} widget for selection of the flowType-field of this flow
	 */
	private FlowTypeViewer flowTypeViewer;

	/**
	 * A {@link ComboViewer} widget for selection of the
	 * referenceFlowProperty-field of this flow
	 */
	private FlowPropertyViewer referenceFlowPropertyViewer;

	/**
	 * Creates a new flow property wizard page
	 */
	public FlowWizardPage() {
		super("FlowWizardPage");
		setTitle(Messages.Flows_WizardTitle);
		setMessage(Messages.Flows_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_FLOW.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null) {
			if (referenceFlowPropertyViewer.getSelected() == null) {
				setErrorMessage(Messages.Flows_EmptyReferenceFlowPropertyError);
			}
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		UI.formLabel(container, Messages.Flows_FlowType);
		flowTypeViewer = new FlowTypeViewer(container);
		flowTypeViewer.select(FlowType.ElementaryFlow);

		UI.formLabel(container, Messages.Flows_ReferenceFlowProperty);
		referenceFlowPropertyViewer = new FlowPropertyViewer(container);
		referenceFlowPropertyViewer.setInput(getDatabase());
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		referenceFlowPropertyViewer
				.addSelectionChangedListener(new ISelectionChangedListener<FlowPropertyDescriptor>() {

					@Override
					public void selectionChanged(FlowPropertyDescriptor selected) {
						checkInput();
					}

				});
	}

	@Override
	public Object[] getData() {
		final Flow flow = new Flow(UUID.randomUUID().toString(),
				getComponentName());
		flow.setCategoryId(getCategoryId());
		flow.setDescription(getComponentDescription());
		flow.setFlowType(flowTypeViewer.getSelected());

		try {
			// load reference flow property
			String id = referenceFlowPropertyViewer.getSelected().getId();
			FlowProperty referenceFlowProperty = getDatabase().select(
					FlowProperty.class, id);
			flow.setReferenceFlowProperty(referenceFlowProperty);

			flow.add(new FlowPropertyFactor(UUID.randomUUID().toString(),
					referenceFlowProperty, 1));

		} catch (final Exception e) {
			setErrorMessage(e.getMessage());
		}
		return new Object[] { flow };
	}
}
