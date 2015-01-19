package org.openlca.app.editors.systems;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ExchangeViewer;
import org.openlca.app.viewers.combo.FlowPropertyFactorViewer;
import org.openlca.app.viewers.combo.UnitViewer;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

class ProductSystemInfoPage extends ModelPage<ProductSystem> {

	private FormToolkit toolkit;
	private ExchangeViewer productViewer;
	private FlowPropertyFactorViewer propertyViewer;
	private UnitViewer unitViewer;
	private Text targetAmountText;

	ProductSystemInfoPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ProductSystem
				+ ": " + getModel().getName());
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, getEditor());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		addCalculationButton(infoSection.getContainer());
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Reference);

		createLink(Messages.Process, "referenceProcess", composite);

		toolkit.createLabel(composite, Messages.Product);
		productViewer = new ExchangeViewer(composite, ExchangeViewer.OUTPUTS,
				ExchangeViewer.PRODUCTS);

		toolkit.createLabel(composite, Messages.FlowProperty);
		propertyViewer = new FlowPropertyFactorViewer(composite);

		toolkit.createLabel(composite, Messages.Unit);
		unitViewer = new UnitViewer(composite);

		productViewer.addSelectionChangedListener(new ProductChangedListener(
				propertyViewer));
		propertyViewer.addSelectionChangedListener(new PropertyChangedListener(
				unitViewer));

		productViewer.setInput(getModel().getReferenceProcess());

		getBinding().onModel(() -> getModel(), "referenceExchange",
				productViewer);
		getBinding().onModel(() -> getModel(), "targetFlowPropertyFactor",
				propertyViewer);
		getBinding().onModel(() -> getModel(), "targetUnit", unitViewer);

		targetAmountText = UI.formText(composite,
				getManagedForm().getToolkit(), Messages.TargetAmount);
		getBinding().onDouble(() -> getModel(), "targetAmount",
				targetAmountText);
	}

	private void addCalculationButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, Messages.Calculate,
				SWT.NONE);
		button.setImage(ImageType.CALCULATE_ICON.get());
		Controls.onSelect(button, (e) -> CalculationWizard.open(getModel()));
	}

	private class ProductChangedListener implements
			ISelectionChangedListener<Exchange> {

		private FlowPropertyFactorViewer propertyViewer;

		private ProductChangedListener(FlowPropertyFactorViewer propertyViewer) {
			this.propertyViewer = propertyViewer;
		}

		@Override
		public void selectionChanged(Exchange selection) {
			Flow flow = selection.getFlow();
			propertyViewer.setInput(flow);
			propertyViewer.select(flow.getReferenceFactor());
		}
	}

	private class PropertyChangedListener implements
			ISelectionChangedListener<FlowPropertyFactor> {

		private UnitViewer unitViewer;

		private PropertyChangedListener(UnitViewer unitViewer) {
			this.unitViewer = unitViewer;
		}

		@Override
		public void selectionChanged(FlowPropertyFactor selection) {
			if (selection == null)
				return;
			UnitGroup unitGroup = selection.getFlowProperty().getUnitGroup();
			unitViewer.setInput(unitGroup);
			Unit previousSelection = getModel().getTargetUnit();
			if (unitGroup.getUnits().contains(previousSelection))
				unitViewer.select(previousSelection);
			else
				unitViewer.select(unitGroup.getReferenceUnit());
		}
	}
}
