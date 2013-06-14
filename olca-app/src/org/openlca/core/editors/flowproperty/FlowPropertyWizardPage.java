/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.flowproperty;

import java.util.UUID;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.viewer.FlowPropertyTypeViewer;
import org.openlca.ui.viewer.ISelectionChangedListener;
import org.openlca.ui.viewer.UnitGroupViewer;

/**
 * Wizard page for creating a new flow property object
 * 
 * @author Sebastian Greve
 * 
 */
public class FlowPropertyWizardPage extends ModelWizardPage {

	/**
	 * Combo box for selection the type of the flow property
	 */
	private FlowPropertyTypeViewer flowPropertyTypeViewer;

	/**
	 * A {@link ComboViewer} widget for selection of the unitGroup-field of this
	 * flow property
	 */
	private UnitGroupViewer unitGroupComboViewer;

	/**
	 * Creates a new flow property wizard page
	 */
	public FlowPropertyWizardPage() {
		super("FlowPropertyWizardPage");
		setTitle(Messages.FlowProps_WizardTitle);
		setMessage(Messages.FlowProps_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_PROPERTY.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null) {
			if (unitGroupComboViewer.getSelected() == null) {
				setErrorMessage(Messages.FlowProps_EmptyUnitGroupError);
			}
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		UI.formLabel(container, Messages.FlowProps_FlowPropertyType);
		flowPropertyTypeViewer = new FlowPropertyTypeViewer(container);
		flowPropertyTypeViewer.select(FlowPropertyType.Physical);

		UI.formLabel(container, Messages.UnitGroup);
		unitGroupComboViewer = new UnitGroupViewer(container);
		unitGroupComboViewer.setInput(getDatabase());
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		unitGroupComboViewer
				.addSelectionChangedListener(new ISelectionChangedListener<UnitGroupDescriptor>() {

					@Override
					public void selectionChanged(UnitGroupDescriptor selection) {
						checkInput();
					}

				});
	}

	@Override
	public Object[] getData() {
		final FlowProperty flowProperty = new FlowProperty(UUID.randomUUID()
				.toString(), getComponentName());
		flowProperty.setCategoryId(getCategoryId());
		flowProperty.setDescription(getComponentDescription());
		flowProperty.setUnitGroupId(unitGroupComboViewer.getSelected().getId());
		flowProperty.setFlowPropertyType(flowPropertyTypeViewer.getSelected());
		return new Object[] { flowProperty };
	}

}
