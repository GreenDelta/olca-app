package org.openlca.app.editors.systems;

import java.math.RoundingMode;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.Preferences;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.DQSystemViewer;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.DQSystemDao;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.ProcessingType;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.DQSystemDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.python.google.common.base.Strings;

class DQSettingsPage extends WizardPage {

	private DQSystemViewer processSystemViewer;
	private DQSystemViewer exchangeSystemViewer;
	private TypeCombo<AggregationType> aggregationTypeCombo;
	private TypeCombo<ProcessingType> processingTypeCombo;
	private TypeCombo<RoundingMode> roundingModeCombo;

	private boolean dqSystemsLoaded;
	private AggregationType aggregationType;
	private ProcessingType processingType;
	private RoundingMode roundingMode;

	public DQSettingsPage() {
		super("DQSettingsPage");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = UI.formComposite(parent);
		setControl(container);
		processSystemViewer = createDQSystemViewer(container, "#Process schema:");
		exchangeSystemViewer = createDQSystemViewer(container, "#I/O schema:");
		new Label(container, SWT.NULL).setText("#Aggregation type:");
		aggregationTypeCombo = new TypeCombo<>(container, AggregationType.class);
		aggregationTypeCombo.setInput(AggregationType.values());
		aggregationTypeCombo.addSelectionChangedListener((e) -> aggregationType = aggregationTypeCombo.getSelected());
		new Label(container, SWT.NULL).setText("#Rounding mode:");
		roundingModeCombo = new TypeCombo<>(container, RoundingMode.class);
		roundingModeCombo.setInput(new RoundingMode[] { RoundingMode.HALF_UP, RoundingMode.CEILING });
		roundingModeCombo.addSelectionChangedListener((e) -> roundingMode = roundingModeCombo.getSelected());
		new Label(container, SWT.NULL).setText("#n.a. value handling:");
		processingTypeCombo = new TypeCombo<>(container, ProcessingType.class);
		processingTypeCombo.setInput(ProcessingType.values());
		processingTypeCombo.addSelectionChangedListener((e) -> processingType = processingTypeCombo.getSelected());
		loadDqSystems();
		loadDefaults();
	}

	public DQCalculationSetup getSetup(ProductSystem system) {
		long psId = system.getId();
		DQSystemDao dqDao = new DQSystemDao(Database.get());
		DQSystem pSystem = null;
		DQSystemDescriptor pSystemDesc = processSystemViewer.getSelected();
		if (pSystemDesc != null)
			pSystem = dqDao.getForId(pSystemDesc.getId());
		DQSystem eSystem = null;
		DQSystemDescriptor eSystemDesc = exchangeSystemViewer.getSelected();
		if (eSystemDesc != null)
			eSystem = dqDao.getForId(eSystemDesc.getId());
		return new DQCalculationSetup(psId, aggregationType, roundingMode, processingType, pSystem, eSystem);
	}

	private void loadDefaults() {
		AggregationType aType = getDefaultValue(AggregationType.class,
				AggregationType.WEIGHTED_AVERAGE);
		aggregationTypeCombo.select(aType);
		ProcessingType pType = getDefaultValue(ProcessingType.class,
				ProcessingType.EXCLUDE);
		processingTypeCombo.select(pType);
		RoundingMode rounding = getDefaultValue(RoundingMode.class,
				RoundingMode.HALF_UP);
		roundingModeCombo.select(rounding);
	}

	private DQSystemViewer createDQSystemViewer(Composite parent, String label) {
		UI.formLabel(parent, label);
		DQSystemViewer viewer = new DQSystemViewer(parent);
		viewer.setNullable(true);
		return viewer;
	}

	private void loadDqSystems() {
		if (dqSystemsLoaded)
			return;
		DQSystemDao dao = new DQSystemDao(Database.get());
		CalculationWizard wizard = (CalculationWizard) getWizard();
		processSystemViewer.setInput(dao.getProcessDqSystems(wizard.productSystem.getId()));
		exchangeSystemViewer.setInput(dao.getExchangeDqSystems(wizard.productSystem.getId()));
		DQSystem processSystem = wizard.productSystem.getReferenceProcess().dqSystem;
		DQSystem exchangeSystem = wizard.productSystem.getReferenceProcess().exchangeDqSystem;
		if (processSystem != null) {
			processSystemViewer.select(Descriptors.toDescriptor(processSystem));
		} else {
			processSystemViewer.selectFirst();
		}
		if (exchangeSystem != null) {
			exchangeSystemViewer.select(Descriptors.toDescriptor(exchangeSystem));
		} else {
			exchangeSystemViewer.selectFirst();
		}
		dqSystemsLoaded = true;
	}

	private <T extends Enum<T>> T getDefaultValue(Class<T> type, T defaultValue) {
		String name = Preferences.get("calc." + type.getSimpleName());
		if (Strings.isNullOrEmpty(name))
			return defaultValue;
		T value = Enum.valueOf(type, name);
		if (value == null)
			return defaultValue;
		return value;
	}

	private class TypeCombo<T> extends AbstractComboViewer<T> {

		private Class<T> clazz;

		protected TypeCombo(Composite parent, Class<T> clazz) {
			super(parent);
			this.clazz = clazz;
		}

		@Override
		public Class<T> getType() {
			return clazz;
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return new LabelProvider();
		}

	}

	private class LabelProvider extends BaseLabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof ProcessingType)
				return Labels.processingType((ProcessingType) element);
			if (element instanceof AggregationType)
				return Labels.aggregationType((AggregationType) element);
			if (element instanceof RoundingMode)
				return Labels.roundingMode((RoundingMode) element);
			return super.getText(element);
		}

	}

}
