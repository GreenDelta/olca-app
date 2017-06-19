package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowPropertyFactorViewer;
import org.openlca.app.viewers.combo.UnitViewer;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.util.Strings;

class ProductSystemInfoPage extends ModelPage<ProductSystem> {

	private ExchangeViewer productViewer;
	private FlowPropertyFactorViewer propertyViewer;
	private UnitViewer unitViewer;
	private Text targetAmountText;
	private ScrolledForm form;

	ProductSystemInfoPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, getEditor());
		FormToolkit tk = managedForm.getToolkit();
		Composite body = UI.formBody(form, tk);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, tk);
		addCalculationButton(infoSection.getContainer(), tk);
		createReferenceSection(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.ProductSystem + ": " + getModel().getName());
	}

	private void createReferenceSection(Composite body, FormToolkit tk) {
		Composite composite = UI.formSection(body, tk, M.Reference);
		createLink(M.Process, "referenceProcess", composite);
		tk.createLabel(composite, M.Product);
		productViewer = new ExchangeViewer(composite);
		tk.createLabel(composite, M.FlowProperty);
		propertyViewer = new FlowPropertyFactorViewer(composite);
		tk.createLabel(composite, M.Unit);
		unitViewer = new UnitViewer(composite);

		productViewer.addSelectionChangedListener(e -> {
			Flow flow = e.getFlow();
			propertyViewer.setInput(flow);
			propertyViewer.select(flow.getReferenceFactor());
		});
		propertyViewer.addSelectionChangedListener(this::propertyChanged);

		productViewer.setInput(getRefCandidates(getModel().getReferenceProcess()));
		targetAmountText = UI.formText(composite,
				getManagedForm().getToolkit(), M.TargetAmount);
		createBindings();
	}

	private void createBindings() {
		getBinding().onModel(() -> getModel(), "referenceExchange",
				productViewer);
		getBinding().onModel(() -> getModel(), "targetFlowPropertyFactor",
				propertyViewer);
		getBinding().onModel(() -> getModel(), "targetUnit", unitViewer);
		getBinding().onDouble(() -> getModel(), "targetAmount",
				targetAmountText);
	}

	private List<Exchange> getRefCandidates(Process p) {
		if (p == null)
			return Collections.emptyList();
		List<Exchange> candidates = new ArrayList<>();
		for (Exchange e : p.getExchanges()) {
			if (e.getFlow() == null)
				continue;
			FlowType type = e.getFlow().getFlowType();
			if (e.isInput() && type == FlowType.WASTE_FLOW)
				candidates.add(e);
			else if (!e.isInput() && type == FlowType.PRODUCT_FLOW)
				candidates.add(e);
		}
		Collections.sort(candidates, (e1, e2) -> Strings.compare(
				Labels.getDisplayName(e1.getFlow()),
				Labels.getDisplayName(e2.getFlow())));
		return candidates;
	}

	private void addCalculationButton(Composite comp, FormToolkit tk) {
		tk.createLabel(comp, "");
		Button button = tk.createButton(comp, M.Calculate, SWT.NONE);
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, e -> CalculationWizard.open(getModel()));
	}

	private void propertyChanged(FlowPropertyFactor f) {
		if (f == null)
			return;
		UnitGroup unitGroup = f.getFlowProperty().getUnitGroup();
		unitViewer.setInput(unitGroup);
		Unit previousSelection = getModel().getTargetUnit();
		if (unitGroup.getUnits().contains(previousSelection))
			unitViewer.select(previousSelection);
		else
			unitViewer.select(unitGroup.getReferenceUnit());
	}

	private class ExchangeViewer extends AbstractComboViewer<Exchange> {

		public ExchangeViewer(Composite parent) {
			super(parent);
		}

		@Override
		public Class<Exchange> getType() {
			return Exchange.class;
		}
	}
}
