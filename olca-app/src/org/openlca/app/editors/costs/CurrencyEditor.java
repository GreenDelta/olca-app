package org.openlca.app.editors.costs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.Currency;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyEditor extends ModelEditor<Currency> {

	private Logger log = LoggerFactory.getLogger(getClass());

	public CurrencyEditor() {
		super(Currency.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	private class Page extends ModelPage<Currency> {

		private CurrencyTable table;
		private CurrencyEditor editor;
		private ScrolledForm form;

		private Page() {
			super(CurrencyEditor.this, "CurrencyPage",
					M.GeneralInformation);
			editor = CurrencyEditor.this;
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			form = UI.formHeader(managedForm);
			updateFormTitle();
			FormToolkit tk = managedForm.getToolkit();
			Composite body = UI.formBody(form, tk);
			InfoSection infoSection = new InfoSection(getEditor());
			infoSection.render(body, tk);
			createAdditionalInfo(body, tk);
			table = new CurrencyTable(getModel());
			table.create(body, tk);
			body.setFocus();
			form.reflow(true);
		}

		@Override
		protected void updateFormTitle() {
			if (form == null)
				return;
			form.setText(M.CostCategory + ": " + getModel().getName());
		}

		private void createAdditionalInfo(Composite body, FormToolkit tk) {
			Composite comp = UI.formSection(body, tk,
					M.AdditionalInformation);
			Text codeText = UI.formText(comp, tk, M.CurrencyCode);
			if (getModel().code != null)
				codeText.setText(getModel().code);
			codeText.addModifyListener(e -> {
				getModel().code = codeText.getText();
				table.refresh();
				editor.setDirty(true);
			});
			Text factorText = UI.formText(comp, tk, M.ConversionFactor);
			factorText.setText(Double.toString(getModel().conversionFactor));
			factorText.addModifyListener(e -> {
				try {
					getModel().conversionFactor = Double.parseDouble(
							factorText.getText());
					table.refresh();
					editor.setDirty(true);
				} catch (Exception ex) {
					log.trace("not a number (currency conversion factor)", e);
				}
			});
			createRefLink(comp, tk);
			createRefButton(comp, tk);
		}

		private void createRefLink(Composite comp, FormToolkit tk) {
			UI.formLabel(comp, tk, M.ReferenceCurrency);
			Currency ref = getModel().referenceCurrency;
			if (ref == null || ref.getName() == null)
				return;
			ImageHyperlink link = tk.createImageHyperlink(comp, SWT.TOP);
			link.setText(ref.getName());
			link.setImage(Images.get(ModelType.CURRENCY));
			Controls.onClick(link, e -> App.openEditor(ref));
		}

		private void createRefButton(Composite comp, FormToolkit tk) {
			UI.formLabel(comp, tk, "");
			Button button = tk.createButton(comp, M.SetAsReferenceCurrency,
					SWT.NONE);
			button.setImage(Images.get(ModelType.CURRENCY));
			Controls.onSelect(button, e -> {
				RefCurrencyUpdate.run(getModel());
			});
		}

	}

}
