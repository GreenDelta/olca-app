/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.unitgroup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.database.Query;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard page for creating a new unit group object
 */
public class UnitGroupWizardPage extends ModelWizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Text referenceUnitText;

	/**
	 * Creates a new unit group wizard page
	 */
	public UnitGroupWizardPage() {
		super("UnitGroupWizardPage");
		setTitle(Messages.Units_WizardTitle);
		setMessage(Messages.Units_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_UNIT.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
		referenceUnitText = UIFactory.createTextWithLabel(container,
				Messages.Units_ReferenceUnit, false);
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		referenceUnitText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				checkInput();
			}
		});
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		String errorMessage = getErrorMessage();
		if (errorMessage != null)
			failCheck(errorMessage);
		else
			checkUnit();
	}

	private void checkUnit() {
		String refUnitName = referenceUnitText.getText().trim();
		if (refUnitName.length() == 0)
			failCheck(Messages.Units_EmptyReferenceUnitError);
		else {
			UnitGroup unitGroup = findGroupWithUnit(refUnitName);
			if (unitGroup != null)
				failCheck(NLS.bind(Messages.Units_UnitExistsError,
						unitGroup.getName()));
			else
				setPageComplete(true);
		}
	}

	private void failCheck(String errorMessage) {
		log.trace("could not create unit group: {}", errorMessage);
		setErrorMessage(errorMessage);
		setPageComplete(false);
	}

	private UnitGroup findGroupWithUnit(String unitName) {
		try {
			String jpql = "select ug from UnitGroup ug join ug.units u where "
					+ "u.name = :unitName";
			Map<String, Object> params = new HashMap<>();
			params.put("unitName", unitName);
			return Query.on(getDatabase()).getFirst(UnitGroup.class, jpql,
					params);
		} catch (Exception e) {
			log.error("Find unit group failed", e);
			return null;
		}
	}

	@Override
	public Object[] getData() {
		UnitGroup unitGroup = new UnitGroup(UUID.randomUUID().toString(),
				getComponentName());
		unitGroup.setCategoryId(getCategoryId());
		unitGroup.setDescription(getComponentDescription());
		Unit referenceUnit = new Unit();
		referenceUnit.setId(UUID.randomUUID().toString());
		referenceUnit.setName(referenceUnitText.getText().trim());
		unitGroup.setReferenceUnit(referenceUnit);
		unitGroup.add(referenceUnit);
		return new Object[] { unitGroup };
	}

}
