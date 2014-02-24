package org.openlca.app.editors.processes;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.util.TableColumnSorter;
import org.openlca.app.util.Tables;
import org.openlca.core.model.Exchange;

import com.google.common.primitives.Doubles;

class ExchangeTableSorters {

	public static void register(TableViewer viewer, ITableLabelProvider provider) {
		// @formatter:off
		Tables.registerSorters(viewer, 
				new TableColumnSorter<>(Exchange.class, 0, provider),
				new TableColumnSorter<>(Exchange.class, 1, provider),
				new TableColumnSorter<>(Exchange.class, 2, provider),
				new TableColumnSorter<>(Exchange.class, 3, provider),
				new AmountSorter(),
				new TableColumnSorter<>(Exchange.class, 5, provider),
				new TableColumnSorter<>(Exchange.class, 6, provider),
				new TableColumnSorter<>(Exchange.class, 7, provider));
		// @formatter:on
	}

	static class AmountSorter extends TableColumnSorter<Exchange> {
		public AmountSorter() {
			super(Exchange.class, 4);
		}

		@Override
		public int compare(Exchange obj1, Exchange obj2) {
			return Doubles
					.compare(obj1.getAmountValue(), obj2.getAmountValue());
		}
	}

}
