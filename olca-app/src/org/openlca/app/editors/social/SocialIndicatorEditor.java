package org.openlca.app.editors.social;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.app.viewers.combo.UnitViewer;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocialIndicatorEditor extends ModelEditor<SocialIndicator> {

	public static String ID = "editors.socialindicator";
	private Logger log = LoggerFactory.getLogger(getClass());

	public SocialIndicatorEditor() {
		super(SocialIndicator.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	private class Page extends ModelPage<SocialIndicator> {

		private SocialIndicatorEditor editor;
		private FlowPropertyViewer quantityCombo;
		private UnitViewer unitCombo;
		private ScrolledForm form;

		Page() {
			super(SocialIndicatorEditor.this, "SocialIndicatorPage", M.GeneralInformation);
			editor = SocialIndicatorEditor.this;
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			form = UI.formHeader(this);
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			InfoSection infoSection = new InfoSection(getEditor());
			infoSection.render(body, toolkit);
			createAdditionalInfo(body, toolkit);
			createActivitySection(toolkit, body);
			body.setFocus();
			form.reflow(true);
		}

		private void createAdditionalInfo(Composite body, FormToolkit tk) {
			Composite comp = UI.formSection(body, tk, M.AdditionalInformation);
			Text ut = UI.formText(comp, tk, M.UnitOfMeasurement);
			if (getModel().unitOfMeasurement != null)
				ut.setText(getModel().unitOfMeasurement);
			ut.addModifyListener((e) -> {
				getModel().unitOfMeasurement = ut.getText();
				editor.setDirty(true);
			});
			Text et = UI.formMultiText(comp, tk, M.EvaluationSchema);
			if (getModel().evaluationScheme != null)
				et.setText(getModel().evaluationScheme);
			et.addModifyListener((e) -> {
				getModel().evaluationScheme = et.getText();
				editor.setDirty(true);
			});
		}

		private void createActivitySection(FormToolkit tk, Composite body) {
			Composite comp = UI.formSection(body, tk,
					M.ActivityVariable);
			Text t = UI.formText(comp, tk, M.Name);
			if (getModel().activityVariable != null)
				t.setText(getModel().activityVariable);
			t.addModifyListener((e) -> {
				getModel().activityVariable = t.getText();
				editor.setDirty(true);
			});
			createQuantityCombo(tk, comp);
			createUnitCombo(tk, comp);
		}

		private void createQuantityCombo(FormToolkit tk, Composite comp) {
			UI.formLabel(comp, tk, M.Quantity);
			quantityCombo = new FlowPropertyViewer(comp);
			quantityCombo.setInput(Database.get());
			FlowProperty aq = getModel().activityQuantity;
			if (aq != null) {
				FlowPropertyDescriptor d = Descriptors.toDescriptor(aq);
				quantityCombo.select(d);
			}
			quantityCombo.addSelectionChangedListener(this::quantityChanged);
		}

		private void createUnitCombo(FormToolkit tk, Composite comp) {
			UI.formLabel(comp, tk, M.Unit);
			unitCombo = new UnitViewer(comp);
			FlowProperty fp = getModel().activityQuantity;
			if (fp != null && fp.getUnitGroup() != null)
				unitCombo.setInput(fp.getUnitGroup().getUnits());
			Unit u = getModel().activityUnit;
			if (u != null)
				unitCombo.select(u);
			unitCombo.addSelectionChangedListener((newUnit) -> {
				getModel().activityUnit = newUnit;
				editor.setDirty(true);
			});
		}

		private void quantityChanged(FlowPropertyDescriptor d) {
			if (d == null)
				return;
			FlowPropertyDao dao = new FlowPropertyDao(Database.get());
			FlowProperty fp = dao.getForId(d.getId());
			if (fp == null)
				return;
			getModel().activityQuantity = fp;
			UnitGroup ug = fp.getUnitGroup();
			if (ug == null)
				return;
			getModel().activityUnit = ug.getReferenceUnit();
			unitCombo.setInput(ug.getUnits());
			unitCombo.select(ug.getReferenceUnit());
			editor.setDirty(true);
		}
	}
}
