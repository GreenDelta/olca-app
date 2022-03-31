package org.openlca.app.db.tables;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.Currency;
import org.openlca.core.model.ModelType;

public class CurrencyTable extends SimpleFormEditor {

	private List<Currency> currencies;

	public static void show() {
		if (Database.get() == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var id = "DbCurrencyTable";
		Editors.open(new SimpleEditorInput(id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		try {
			currencies = Database.get().getAll(Currency.class);
		} catch (Exception e) {
			ErrorReporter.on("failed to load currencies", e);
		}
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final List<Currency> currencies;

		Page(CurrencyTable table) {
			super(table, "DbCurrencyTable", M.Currencies);
			currencies = table.currencies != null
				? table.currencies
				: Collections.emptyList();
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, M.Currencies);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var filterComp = tk.createComposite(body);
			UI.gridLayout(filterComp, 2);
			UI.gridData(filterComp, true, false);
			var filter = UI.formText(filterComp, tk, M.Filter);

			var table = Tables.createViewer(body,
				M.Name,
				M.Code,
				"Exchange rate",
				"ID");
			Tables.bindColumnWidths(table, 0.3, 0.1, 0.2, 0.4);

			var label = new Label();
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1, 2, 3);
			table.setInput(currencies);
			TextFilter.on(table, filter);
			Actions.bind(table);
		}
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0
				? Images.get(ModelType.CURRENCY)
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Currency currency))
				return null;
			return switch (col) {
				case 0 -> currency.name;
				case 1 -> currency.code;
				case 2 -> getExchangeRate(currency, currency.referenceCurrency);
				case 3 -> currency.refId;
				default -> null;
			};
		}

		private String getExchangeRate(Currency currency, Currency refCurrency) {
			String s = "1 " + currency.code + " = ";
			double f = currency.conversionFactor / refCurrency.conversionFactor;
			f = Math.round(1000 * f) / 1000.0;
			s += f + " " + refCurrency.code;
			return s;
		}
	}

}
