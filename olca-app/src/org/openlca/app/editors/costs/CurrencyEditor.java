package org.openlca.app.editors.costs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
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
			super(CurrencyEditor.this, "CurrencyPage", M.GeneralInformation);
			editor = CurrencyEditor.this;
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			form = UI.formHeader(this);
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

		private void createAdditionalInfo(Composite body, FormToolkit tk) {
			Composite comp = UI.formSection(body, tk, M.AdditionalInformation, 3);
			Text codeText = text(comp, M.CurrencyCode, "code");
			codeText.addModifyListener(e -> {
				table.refresh();
				editor.setDirty(true);
			});
			Text factorText = doubleText(comp, M.ConversionFactor, "conversionFactor");
			factorText.addModifyListener(e -> {
				table.refresh();
				editor.setDirty(true);
			});
			link(comp, M.ReferenceCurrency, "referenceCurrency");
			createRefButton(comp, tk);
		}

		private void createRefButton(Composite comp, FormToolkit tk) {
			UI.filler(comp, tk);
			Button b = tk.createButton(comp, M.SetAsReferenceCurrency, SWT.NONE);
			b.setImage(Images.get(ModelType.CURRENCY));
			Controls.onSelect(b, e -> {
				RefCurrencyUpdate.run(getModel());
			});
			UI.filler(comp, tk);
		}
	}
}
