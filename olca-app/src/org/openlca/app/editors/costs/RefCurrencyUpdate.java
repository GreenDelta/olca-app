package org.openlca.app.editors.costs;

import java.util.Calendar;
import java.util.Objects;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.util.Question;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CurrencyDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Changes the reference currency of the database. This is called from the
 * currency editor of the currency that should be the next reference currency of
 * the database. All currency editors are closed and the editor of reference
 * currency is reopened after the update.
 */
class RefCurrencyUpdate implements Runnable {

	private Currency c;

	private RefCurrencyUpdate(Currency c) {
		this.c = c;
	}

	static void run(Currency c) {
		if (c == null)
			return;
		if (Objects.equals(c, c.referenceCurrency))
			return;
		boolean b = Question.ask(M.SetAsReferenceCurrency,
				M.SetReferenceCurrencyQuestion);
		if (!b)
			return;
		closeEditors();
		App.run(M.UpdateReferenceCurrency,
				new RefCurrencyUpdate(c), () -> {
					CurrencyDescriptor d = Descriptors.toDescriptor(c);
					App.openEditor(d);
				});
	}

	private static void closeEditors() {
		for (IEditorReference ref : Editors.getReferences()) {
			IEditorPart editor = ref.getEditor(false);
			if (editor instanceof CurrencyEditor)
				Editors.close(ref);
		}
	}

	@Override
	public void run() {
		try {
			CurrencyDao dao = new CurrencyDao(Database.get());
			c.lastChange = Calendar.getInstance().getTimeInMillis();
			Version.incUpdate(c);
			c = dao.update(c);
			double f = c.conversionFactor;
			for (Currency o : dao.getAll()) {
				o.referenceCurrency = c;
				o.lastChange = Calendar.getInstance().getTimeInMillis();
				Version.incUpdate(o);
				if (Objects.equals(c, o)) {
					o.conversionFactor = 1.0;
					c = dao.update(o);
				} else {
					o.conversionFactor = o.conversionFactor / f;
					dao.update(o);
				}
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to update reference currency", e);
		}
	}

}
