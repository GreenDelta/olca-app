package org.openlca.app.editors.costs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.util.Strings;

class CurrencyTable {

	private Currency currency;
	private TableViewer table;

	CurrencyTable(Currency currency) {
		this.currency = currency;
	}

	void create(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk, "Other currencies");
		UI.gridLayout(comp, 1);
		table = Tables.createViewer(comp, "Name", "Code", "Exchange rate");
		Tables.bindColumnWidths(table, 0.4, 0.2, 0.4);
		table.setLabelProvider(new Label());
		table.setInput(getOthers());
	}

	private List<Currency> getOthers() {
		CurrencyDao dao = new CurrencyDao(Database.get());
		List<Currency> others = new ArrayList<>();
		for (Currency c : dao.getAll()) {
			if (Objects.equals(c, currency))
				continue;
			if (!Objects.equals(c.referenceCurrency, currency.referenceCurrency))
				continue;
			others.add(c);
		}
		Collections.sort(others, (c1, c2) -> {
			return Strings.compare(c1.getName(), c2.getName());
		});
		return others;
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Currency))
				return null;
			Currency c = (Currency) obj;
			switch (col) {
			case 0:
				return c.getName();
			case 1:
				return c.code;
			case 2:
				return getExchangeRate(c);
			default:
				return null;
			}
		}

		private String getExchangeRate(Currency c) {
			String s = "1 " + currency.code + " = ";
			double f = Math.round(100 * c.conversionFactor / currency.conversionFactor) / 100.0;
			s += f;
			s += " " + c.code;
			return s;
		}

	}

}
