package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Processes;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Clipboard {

	static List<Exchange> read(String text, boolean forInputs) {
		if (text == null)
			return Collections.emptyList();
		String[] lines = text.toString().split("\n");
		List<Exchange> list = new ArrayList<>();
		Mapper mapper = new Mapper();
		for (String line : lines) {
			String[] row = line.split("\t");
			for (int k = 0; k < row.length; k++) {
				row[k] = row[k].trim();
			}
			if (row.length > 2 && Strings.nullOrEqual(row[2], M.Amount))
				continue; // the header row
			Exchange e = mapper.doIt(row, forInputs);
			if (e != null) {
				list.add(e);
			}
		}
		return list;
	}

	private static class Mapper {
		final Logger log = LoggerFactory.getLogger(getClass());
		final FlowDao flowDao = new FlowDao(Database.get());
		final CurrencyDao currencyDao = new CurrencyDao(Database.get());

		Exchange doIt(String[] row, boolean isInput) {
			if (row == null || row.length == 0)
				return null;
			log.trace("create exchange '{}' from clipboard", row[0]);
			Flow flow = findFlow(row);
			if (flow == null)
				return null;
			Exchange e = new Exchange();
			e.flow = flow;
			e.isInput = isInput;
			mapAmount(e, row);
			mapUnit(e, row);
			mapCosts(e, row);
			if (row.length > 5)
				e.uncertainty = Uncertainty.fromString(row[5]);
			mapIsAvoided(e, row);
			mapProvider(e, row);
			if (row.length > 8 && !Strings.nullOrEmpty(row[8]))
				e.dqEntry = row[8];
			if (row.length > 9)
				e.description = row[9];
			return e;
		}

		Flow findFlow(String[] row) {
			String fullName = row[0];
			if (fullName == null)
				return null;
			List<Flow> candidates = new ArrayList<>();
			candidates.addAll(flowDao.getForName(fullName));

			// the full name may contains a location code
			String name = null;
			String locationCode = null;
			if (fullName.contains(" - ")) {
				int splitIdx = fullName.lastIndexOf(" - ");
				name = fullName.substring(0, splitIdx).trim();
				locationCode = fullName.substring(splitIdx + 3).trim();
				candidates.addAll(flowDao.getForName(name));
			}

			if (candidates.isEmpty()) {
				log.warn("Could not find flow '{}' in database", fullName);
				return null;
			}
			Flow selected = null;
			for (Flow candidate : candidates) {
				if (selected == null) {
					selected = candidate;
					continue;
				}
				if (matchCategory(candidate, row)
						&& !matchCategory(selected, row)) {
					selected = candidate;
					continue;
				}
				if (matchLocation(candidate, locationCode)
						&& !matchLocation(selected, locationCode)) {
					selected = candidate;
					continue;
				}
			}
			return selected;
		}

		private boolean matchCategory(Flow flow, String[] row) {
			if (flow == null)
				return false;
			if (row.length < 2 || Strings.nullOrEmpty(row[1]))
				return flow.category == null;
			String[] names = row[1].split("/");
			Category category = flow.category;
			for (int i = names.length - 1; i >= 0; i--) {
				if (category == null)
					return false;
				if (!Strings.nullOrEqual(names[i], category.name))
					return false;
				category = category.category;
			}
			return true;
		}

		private boolean matchLocation(Flow flow, String code) {
			if (flow == null)
				return false;
			if (flow.location == null)
				return code == null;
			if (code == null)
				return flow.location == null;
			return Strings.nullOrEqual(flow.location.code, code);
		}

		private void mapAmount(Exchange e, String[] row) {
			if (row.length < 3) {
				e.amount = 1.0;
				return;
			}
			try {
				e.amount = Double.parseDouble(row[2]);
			} catch (Exception ex) {
				e.amountFormula = row[2];
			}
		}

		private void mapUnit(Exchange e, String[] row) {
			if (row.length < 4) {
				setRefUnit(e);
				return;
			}
			if (e.flow == null)
				return;
			String unit = row[3];
			if (mapUnit(e, e.flow.getReferenceFactor(), unit))
				return;
			for (FlowPropertyFactor f : e.flow.flowPropertyFactors) {
				if (mapUnit(e, f, unit))
					return;
			}
			setRefUnit(e);
		}

		private void setRefUnit(Exchange e) {
			if (e == null || e.flow == null)
				return;
			FlowPropertyFactor refFactor = e.flow.getReferenceFactor();
			e.flowPropertyFactor = refFactor;
			if (refFactor == null || refFactor.flowProperty == null)
				return;
			UnitGroup ug = refFactor.flowProperty.unitGroup;
			if (ug == null)
				return;
			e.unit = ug.referenceUnit;
		}

		private boolean mapUnit(Exchange e, FlowPropertyFactor factor, String unit) {
			if (e == null || factor == null)
				return false;
			FlowProperty fp = factor.flowProperty;
			if (fp == null || fp.unitGroup == null)
				return false;
			Unit u = fp.unitGroup.getUnit(unit);
			if (u == null)
				return false;
			e.flowPropertyFactor = factor;
			e.unit = u;
			return true;
		}

		/**
		 * It is important that the flow direction is already specified before
		 * calling this method because if the exchange is an avoided product or
		 * waste flow the flow direction will change in this method.
		 */
		private void mapIsAvoided(Exchange e, String[] row) {
			if (e == null || e.flow == null || row.length < 7)
				return;
			String s = row[6];
			if (Strings.nullOrEmpty(s))
				return;
			if ((e.isInput && e.flow.flowType != FlowType.WASTE_FLOW)
					|| (!e.isInput && e.flow.flowType != FlowType.PRODUCT_FLOW))
				return;
			try {
				if (Boolean.parseBoolean(s)) {
					e.isAvoided = true;
					e.isInput = !e.isInput;
				}
			} catch (Exception ex) {
			}
		}

		private void mapCosts(Exchange e, String[] row) {
			if (e == null || row.length < 5)
				return;
			String s = row[4];
			if (Strings.nullOrEmpty(s))
				return;
			s = s.trim();
			int splitIdx = s.lastIndexOf(' ');
			if (splitIdx < 0)
				return;
			String amount = s.substring(0, splitIdx).trim();
			String currencySymbol = s.substring(splitIdx + 1).trim();
			Currency currency = null;
			for (Currency cu : currencyDao.getAll()) {
				if (Strings.nullOrEqual(currencySymbol, cu.code)) {
					currency = cu;
					break;
				}
			}
			if (currency == null) {
				log.warn("Could not find currency for symbol={}",
						currencySymbol);
				return;
			}
			e.currency = currency;
			try {
				e.costs = Double.parseDouble(amount);
			} catch (Exception ex) {
				e.costFormula = amount;
			}
		}

		private void mapProvider(Exchange e, String[] row) {
			if (e == null || e.flow == null || row.length < 8)
				return;
			if ((e.isInput && e.flow.flowType != FlowType.PRODUCT_FLOW)
					|| (!e.isInput && e.flow.flowType != FlowType.WASTE_FLOW))
				return;
			String fullName = row[7];
			if (Strings.nullOrEmpty(fullName))
				return;
			ProcessDescriptor d = Processes.findForLabel(Database.get(), fullName);
			if (d == null) {
				log.warn("Could not find provider '{}' in database", fullName);
			} else {
				e.defaultProviderId = d.id;
			}
		}
	}
}
