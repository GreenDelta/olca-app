package org.openlca.app.wizards.calculation;

import java.math.RoundingMode;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.DQSystemViewer;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.DQSystemDao;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.ProcessingType;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;

class DQSettingsPage extends WizardPage {

	private final Setup setup;

	public DQSettingsPage(Setup setup) {
		super("DQSettingsPage");
		this.setup = setup;
		setTitle(M.DataQualityProperties);
		setDescription(M.PleaseSelectProperties);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = UI.formComposite(parent);
		setControl(container);

		// DQ systems
		createDQViewer(container, false);
		createDQViewer(container, true);

		// aggregation type
		new Label(container, SWT.NULL).setText(M.AggregationType);
		TypeCombo<AggregationType> aggCombo = new TypeCombo<>(
				container, AggregationType.class);
		aggCombo.setInput(AggregationType.values());
		aggCombo.select(setup.dqSetup.aggregationType);
		aggCombo.addSelectionChangedListener(_e -> {
			setup.dqSetup.aggregationType = aggCombo.getSelected();
		});

		// rounding mode
		new Label(container, SWT.NULL).setText(M.RoundingMode);
		TypeCombo<RoundingMode> roundCombo = new TypeCombo<>(
				container, RoundingMode.class);
		roundCombo.setInput(new RoundingMode[] {
				RoundingMode.HALF_UP,
				RoundingMode.CEILING });
		roundCombo.select(setup.dqSetup.roundingMode);
		roundCombo.addSelectionChangedListener(_e -> {
			setup.dqSetup.roundingMode = roundCombo.getSelected();
		});

		// n.a. handling
		new Label(container, SWT.NULL).setText(M.NaValueHandling);
		TypeCombo<ProcessingType> naCombo = new TypeCombo<>(
				container, ProcessingType.class);
		naCombo.setInput(ProcessingType.values());
		naCombo.addSelectionChangedListener(_e -> {
			setup.dqSetup.processingType = naCombo.getSelected();
		});
	}

	private void createDQViewer(Composite comp, boolean forExchanges) {
		UI.formLabel(comp, forExchanges
				? M.FlowSchema
				: M.ProcessSchema);
		DQSystemViewer combo = new DQSystemViewer(comp);
		combo.setNullable(true);
		DQSystemDao dao = new DQSystemDao(Database.get());
		ProductSystem system = setup.calcSetup.productSystem;
		if (forExchanges) {
			combo.setInput(dao.getExchangeDqSystems(system.id));
		} else {
			combo.setInput(dao.getProcessDqSystems(system.id));
		}
		DQSystem selected = forExchanges
				? system.referenceProcess.exchangeDqSystem
				: system.referenceProcess.dqSystem;
		if (selected != null) {
			combo.select(Descriptors.toDescriptor(selected));
		} else {
			combo.selectFirst();
		}

		combo.addSelectionChangedListener(dqSystem -> {
			DQSystem s = dqSystem == null
					? null
					: new DQSystemDao(Database.get()).getForId(dqSystem.id);
			if (forExchanges) {
				setup.dqSetup.exchangeDqSystem = s;
			} else {
				setup.dqSetup.processDqSystem = s;
			}
		});
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
			return new BaseLabelProvider() {
				@Override
				public String getText(Object o) {
					if (o instanceof ProcessingType)
						return Labels.of((ProcessingType) o);
					if (o instanceof AggregationType)
						return Labels.of((AggregationType) o);
					if (o instanceof RoundingMode)
						return Labels.of((RoundingMode) o);
					return super.getText(o);
				}
			};
		}
	}
}
