package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyWizard extends AbstractWizard<Currency> {

	@Override
	protected BaseDao<Currency> createDao() {
		return Database.createDao(Currency.class);
	}

	@Override
	protected String getTitle() {
		return "#New currency";
	}

	@Override
	protected AbstractWizardPage<Currency> createPage() {
		return new Page();
	}

	private class Page extends AbstractWizardPage<Currency> {

		Page() {
			super("CurrencyWizardPage");
			setTitle("#New currency");
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
		}

		@Override
		public Currency createModel() {
			Currency c = new Currency();
			c.setRefId(UUID.randomUUID().toString());
			c.setName(getModelName());
			c.setDescription(getModelDescription());
			c.conversionFactor = 1.0;
			c.code = getModelName();
			Currency ref = getRefCurrency();
			if (ref != null)
				c.referenceCurrency = ref;
			else
				c.referenceCurrency = c;
			return c;
		}

		private Currency getRefCurrency() {
			try {
				CurrencyDao dao = new CurrencyDao(Database.get());
				return dao.getReferenceCurrency();
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to get the reference currency", e);
				return null;
			}
		}
	}

}
