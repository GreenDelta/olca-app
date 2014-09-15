package org.openlca.app.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UIFactory;
import org.openlca.core.database.Query;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UnitGroupWizardPage extends AbstractWizardPage<UnitGroup> {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Text referenceUnitText;

	public UnitGroupWizardPage() {
		super("UnitGroupWizardPage");
		setTitle(Messages.NewUnitGroup);
		setMessage(Messages.CreatesANewUnitGroup);
		setImageDescriptor(ImageType.NEW_WIZ_UNIT.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
		referenceUnitText = UIFactory.createTextWithLabel(container,
				Messages.ReferenceUnit, false);
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
			failCheck(Messages.ReferenceUnitIsEmptyOrInvalid);
		else {
			UnitGroup unitGroup = findGroupWithUnit(refUnitName);
			if (unitGroup != null)
				failCheck(NLS.bind(Messages.UnitAlreadyExistsInUnitGroup,
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
			return Query.on(Database.get()).getFirst(UnitGroup.class, jpql,
					params);
		} catch (Exception e) {
			log.error("Find unit group failed", e);
			return null;
		}
	}

	public UnitGroup createModel() {
		UnitGroup unitGroup = new UnitGroup();
		unitGroup.setRefId(UUID.randomUUID().toString());
		unitGroup.setName(getModelName());
		unitGroup.setDescription(getModelDescription());
		Unit referenceUnit = new Unit();
		referenceUnit.setRefId(UUID.randomUUID().toString());
		referenceUnit.setName(referenceUnitText.getText().trim());
		unitGroup.setReferenceUnit(referenceUnit);
		unitGroup.getUnits().add(referenceUnit);
		return unitGroup;
	}

}
