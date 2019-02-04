package org.openlca.app.editors.processes.exchanges;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ParameterProposals;
import org.openlca.app.db.Database;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;

class CostDialog extends FormDialog {

	private Process process;
	private Exchange exchange;
	private ComboViewer currencyCombo;
	private Text priceText;
	private Label currencyLabel;
	private Text pricePerUnitText;
	private Label currencyPerUnitLabel;

	private Currency currency;

	public static int open(Process process, Exchange exchange) {
		if (exchange == null)
			return CANCEL;
		CostDialog d = new CostDialog(process, exchange);
		return d.open();
	}

	private CostDialog(Process process, Exchange exchange) {
		super(UI.shell());
		this.exchange = exchange;
		this.process = process;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		UI.formHeader(mform, M.Price);
		Composite body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 3);
		createCurrencyRow(body, tk);
		createCostsRow(body, tk);
		createCostsPerUnitRow(body, tk);
		updateCurrencyLabels();
	}

	private void createCurrencyRow(Composite body, FormToolkit tk) {
		Combo widget = UI.formCombo(body, tk, M.Currency);
		currencyCombo = new ComboViewer(widget);
		currencyCombo.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object obj) {
				if (!(obj instanceof Currency))
					return super.getText(obj);
				return ((Currency) obj).name;
			}
		});
		setCurrencyContent(currencyCombo);
		currencyCombo.addSelectionChangedListener(e -> {
			currency = Viewers.getFirst(e.getSelection());
			exchange.currency = currency;
			updateCurrencyLabels();
		});
		UI.filler(body, tk);
	}

	private void setCurrencyContent(ComboViewer combo) {
		combo.setContentProvider(ArrayContentProvider.getInstance());
		CurrencyDao dao = new CurrencyDao(Database.get());
		List<Currency> all = dao.getAll();
		Collections.sort(all,
				(c1, c2) -> Strings.compare(c1.name, c2.name));
		combo.setInput(all);
		currency = exchange.currency;
		if (currency == null)
			currency = dao.getReferenceCurrency();
		if (currency != null) {
			combo.setSelection(new StructuredSelection(currency));
			exchange.currency = currency;
		}
	}

	private void createCostsRow(Composite body, FormToolkit tk) {
		priceText = UI.formText(body, tk, M.CostsRevenues);
		currencyLabel = UI.formLabel(body, tk, "");
		if (exchange.costFormula != null)
			priceText.setText(exchange.costFormula);
		else if (exchange.costs != null)
			priceText.setText(Double.toString(exchange.costs));
		priceText.addModifyListener(e -> {
			try {
				String s = priceText.getText();
				if (Strings.nullOrEmpty(s))
					exchange.costs = null;
				else
					exchange.costs = Double.parseDouble(s);
				exchange.costFormula = null;
				clearFormulaError();
			} catch (Exception ex) {
				setFormula();
			}
			double price = exchange.costs != null ? exchange.costs : 0;
			double perUnit = price / exchange.amount;
			pricePerUnitText.setText(Double.toString(perUnit));
		});
		ParameterProposals.on(priceText, () -> process.parameters);
	}

	private void setFormula() {
		exchange.costFormula = priceText.getText();
		List<String> errors = Formulas.eval(Database.get(), process);
		if (errors == null || errors.isEmpty())
			clearFormulaError();
		else {
			priceText.setBackground(Colors.errorColor());
			priceText.setToolTipText(errors.get(0));
		}
	}

	private void clearFormulaError() {
		if (priceText == null)
			return;
		priceText.setBackground(Colors.white());
		priceText.setToolTipText("");
	}

	private void createCostsPerUnitRow(Composite body, FormToolkit tk) {
		pricePerUnitText = UI.formText(body, tk, M.CostsPerUnit);
		pricePerUnitText.setEditable(false);
		pricePerUnitText.setBackground(Colors.get(225, 225, 225));
		currencyPerUnitLabel = UI.formLabel(body, tk, "");
		if (exchange.costs != null) {
			double perUnit = exchange.costs / exchange.amount;
			pricePerUnitText.setText(Double.toString(perUnit));
		}
	}

	private void updateCurrencyLabels() {
		String code = currency == null ? "?" : currency.code;
		String unit = exchange.unit == null
				? "?"
				: exchange.unit.name;
		currencyLabel.setText(code);
		currencyLabel.pack();
		currencyPerUnitLabel.setText(code + " / " + unit);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected Point getInitialSize() {
		int width = 500;
		int height = 300;
		Rectangle shellBounds = getShell().getDisplay().getBounds();
		int shellWidth = shellBounds.x;
		int shellHeight = shellBounds.y;
		if (shellWidth > 0 && shellWidth < width)
			width = shellWidth;
		if (shellHeight > 0 && shellHeight < height)
			height = shellHeight;
		return new Point(width, height);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point loc = super.getInitialLocation(initialSize);
		int marginTop = (getParentShell().getSize().y - initialSize.y) / 3;
		if (marginTop < 0)
			marginTop = 0;
		return new Point(loc.x, loc.y + marginTop);
	}

}
