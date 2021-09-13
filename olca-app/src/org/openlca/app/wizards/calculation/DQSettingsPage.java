package org.openlca.app.wizards.calculation;

import java.math.RoundingMode;
import java.util.List;

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
import org.openlca.core.math.data_quality.NAHandling;
import org.openlca.core.model.descriptors.DQSystemDescriptor;
import org.openlca.core.model.descriptors.Descriptor;

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
		aggCombo.addSelectionChangedListener(
			_e -> setup.dqSetup.aggregationType = aggCombo.getSelected());

		// rounding mode
		new Label(container, SWT.NULL).setText(M.RoundingMode);
		var roundCombo = new TypeCombo<>(container, RoundingMode.class);
		roundCombo.setInput(new RoundingMode[]{
			RoundingMode.HALF_UP,
			RoundingMode.CEILING});
		roundCombo.select(setup.dqSetup.ceiling
			? RoundingMode.CEILING
			: RoundingMode.HALF_UP);
		roundCombo.addSelectionChangedListener(_e -> {
			var rmode = roundCombo.getSelected();
			setup.dqSetup.ceiling = rmode == RoundingMode.CEILING;
		});

		// n.a. handling
		new Label(container, SWT.NULL).setText(M.NaValueHandling);
		var naCombo = new TypeCombo<>(container, NAHandling.class);
		naCombo.setInput(NAHandling.values());
		naCombo.addSelectionChangedListener(
			_e -> setup.dqSetup.naHandling = naCombo.getSelected());
	}

	private void createDQViewer(Composite comp, boolean forExchanges) {
		UI.formLabel(comp, forExchanges
			? M.FlowSchema
			: M.ProcessSchema);
		var combo = new DQSystemViewer(comp);
		combo.setNullable(true);

		var dao = new DQSystemDao(Database.get());
		var system = setup.calcSetup.hasProductSystem()
			? setup.calcSetup.productSystem()
			: null;

		List<DQSystemDescriptor> dqSystems;
		if (system == null) {
			dqSystems = dao.getDescriptors();
		} else {
			dqSystems = forExchanges
				? dao.getExchangeDqSystems(system.id)
				: dao.getProcessDqSystems(system.id);
		}
		combo.setInput(dqSystems);

		var selected = forExchanges
			? setup.dqSetup.exchangeSystem
			: setup.dqSetup.processSystem;
		if (selected != null) {
			combo.select(Descriptor.of(selected));
		} else {
			combo.selectFirst();
		}

		combo.addSelectionChangedListener(d -> {
			var dqSystem = d == null
				? null
				: new DQSystemDao(Database.get()).getForId(d.id);
			if (forExchanges) {
				setup.dqSetup.exchangeSystem = dqSystem;
			} else {
				setup.dqSetup.processSystem = dqSystem;
			}
		});
	}

	private static class TypeCombo<T> extends AbstractComboViewer<T> {

		private final Class<T> clazz;

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
					if (o instanceof NAHandling)
						return Labels.of((NAHandling) o);
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
