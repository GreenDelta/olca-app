package org.openlca.app.editors.costs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.Currency;
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
					Messages.GeneralInformation);
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
			form.setText(Messages.CostCategory + ": " + getModel().getName());
		}

		private void createAdditionalInfo(Composite body, FormToolkit tk) {
			Composite comp = UI.formSection(body, tk,
					Messages.AdditionalInformation);
			Text codeText = UI.formText(comp, tk, "#Currency code");
			if (getModel().code != null)
				codeText.setText(getModel().code);
			codeText.addModifyListener(e -> {
				getModel().code = codeText.getText();
				table.refresh();
				editor.setDirty(true);
			});
			Text factorText = UI.formText(comp, tk, "#Conversion factor");
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
			UI.formLabel(comp, tk, "#Reference currency");
			Currency ref = getModel().referenceCurrency;
			if (ref == null || ref.getName() == null)
				return;
			ImageHyperlink link = tk.createImageHyperlink(comp, SWT.TOP);
			link.setText(ref.getName());
			link.setImage(ImageType.CALCULATE_COSTS.get());
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					App.openEditor(ref);
				}
			});
		}

		private void createRefButton(Composite comp, FormToolkit tk) {
			UI.formLabel(comp, tk, "");
			Button button = tk.createButton(comp, "#Set as reference currency",
					SWT.NONE);
			button.setImage(ImageType.CALCULATE_COSTS.get());
			Controls.onSelect(button, e -> {
				RefCurrencyUpdate.run(getModel());
			});
		}

	}

}
