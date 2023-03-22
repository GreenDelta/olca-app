package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowPropertyFactorViewer;
import org.openlca.app.viewers.combo.UnitCombo;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

public class ProductSystemInfoPage extends ModelPage<ProductSystem> {

	public static final String ID = "ProductSystemInfoPage";
	private ExchangeViewer flowCombo;
	private FlowPropertyFactorViewer propertyCombo;
	private UnitCombo unitCombo;
	private Text targetAmountText;


	ProductSystemInfoPage(ProductSystemEditor editor) {
		super(editor, ID, M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var info = new InfoSection(getEditor());
		info.render(body, tk);
		addCalculationButton(info.composite(), tk);
		createReferenceSection(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void createReferenceSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Reference, 3);
		link(comp, M.Process, "referenceProcess");
		UI.label(comp, tk, M.Product);
		flowCombo = new ExchangeViewer(comp);
		flowCombo.addSelectionChangedListener(e -> {
			if (e == null || e.flow == null)
				return;
			propertyCombo.setInput(e.flow);
			propertyCombo.select(e.flow.getReferenceFactor());
		});
		flowCombo.setInput(getRefCandidates(getModel().referenceProcess));
		new CommentControl(comp, getToolkit(), "referenceExchange", getComments());
		UI.label(comp, tk, M.FlowProperty);
		propertyCombo = new FlowPropertyFactorViewer(comp);
		propertyCombo.addSelectionChangedListener(this::propertyChanged);
		new CommentControl(comp, getToolkit(), "targetFlowPropertyFactor", getComments());
		UI.label(comp, tk, M.Unit);
		unitCombo = new UnitCombo(comp);
		new CommentControl(comp, getToolkit(), "targetUnit", getComments());
		targetAmountText = UI.labeledText(comp, getManagedForm().getToolkit(), M.TargetAmount);
		new CommentControl(comp, getToolkit(), "targetAmount", getComments());
		createBindings();
	}

	private void createBindings() {
		getBinding().onModel(this::getModel, "referenceExchange", flowCombo);
		getBinding().onModel(this::getModel, "targetFlowPropertyFactor", propertyCombo);
		getBinding().onModel(this::getModel, "targetUnit", unitCombo);
		getBinding().onDouble(this::getModel, "targetAmount", targetAmountText);
	}

	private List<Exchange> getRefCandidates(Process p) {
		if (p == null)
			return Collections.emptyList();
		var candidates = new ArrayList<Exchange>();
		for (var e : p.exchanges) {
			if (e.isAvoided || e.flow == null)
				continue;
			var type = e.flow.flowType;
			if (e.isInput && type == FlowType.WASTE_FLOW) {
				candidates.add(e);
			} else if (!e.isInput && type == FlowType.PRODUCT_FLOW) {
				candidates.add(e);
			}
		}
		candidates.sort((e1, e2) -> Strings.compare(
				Labels.name(e1.flow), Labels.name(e2.flow)));
		return candidates;
	}

	private void addCalculationButton(Composite comp, FormToolkit tk) {
		UI.label(comp, tk, "");
		var button = UI.button(comp, tk, M.Calculate);
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, e -> CalculationDispatch.call(getModel()));
		UI.label(comp, tk, "");
	}

	private void propertyChanged(FlowPropertyFactor f) {
		if (f == null
				|| f.flowProperty == null
				|| f.flowProperty.unitGroup == null)
			return;
		var group = f.flowProperty.unitGroup;
		unitCombo.setInput(group);
		var current = getModel().targetUnit;
		var next = group.units.contains(current)
				? current
				: group.referenceUnit;
		unitCombo.select(next);
	}

	private static class ExchangeViewer extends AbstractComboViewer<Exchange> {

		public ExchangeViewer(Composite parent) {
			super(parent);
		}

		@Override
		public Class<Exchange> getType() {
			return Exchange.class;
		}
	}
}
