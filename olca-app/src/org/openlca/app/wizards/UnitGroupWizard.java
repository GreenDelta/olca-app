package org.openlca.app.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.core.database.Query;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitGroupWizard extends AbstractWizard<UnitGroup> {

	@Override
	protected String getTitle() {
		return M.NewUnitGroup;
	}

	@Override
	protected AbstractWizardPage<UnitGroup> createPage() {
		return new Page();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.UNIT_GROUP;
	}

	private class Page extends AbstractWizardPage<UnitGroup> {

		private Logger log = LoggerFactory.getLogger(this.getClass());
		private Text referenceUnitText;

		public Page() {
			super("UnitGroupWizardPage");
			setTitle(M.NewUnitGroup);
			setMessage(M.CreatesANewUnitGroup);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
			referenceUnitText = UI.formText(container, M.ReferenceUnit);
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
				failCheck(M.ReferenceUnitIsEmptyOrInvalid);
			else {
				UnitGroup unitGroup = findGroupWithUnit(refUnitName);
				if (unitGroup != null)
					failCheck(NLS.bind(M.UnitAlreadyExistsInUnitGroup,
							unitGroup.name));
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
			unitGroup.refId = UUID.randomUUID().toString();
			unitGroup.name = getModelName();
			unitGroup.description = getModelDescription();
			Unit referenceUnit = new Unit();
			referenceUnit.refId = UUID.randomUUID().toString();
			referenceUnit.name = referenceUnitText.getText().trim();
			unitGroup.referenceUnit = referenceUnit;
			unitGroup.units.add(referenceUnit);
			return unitGroup;
		}

	}

}
