package org.openlca.core.editors.process;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.ui.CategoryPath;
import org.openlca.ui.Labels;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The sorter for the input / output tables. */
class ExchangeSorter extends ViewerSorter {

	private Logger log = LoggerFactory.getLogger(getClass());
	private boolean ascending = true;
	private String property = ExchangeTable.FLOW;

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setSortProperty(String property) {
		this.property = property;
	}

	public String getSortProperty() {
		return property;
	}

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		if (!(o1 instanceof Exchange) || !(o2 instanceof Exchange))
			return 0;
		int c = 0;
		Exchange e1 = (Exchange) o1;
		Exchange e2 = (Exchange) o2;
		if (property.equals(ExchangeTable.FLOW))
			c = compareByFlows(e1, e2);
		else if (property.equals(ExchangeTable.PROPERTY))
			c = compareByProperties(e1, e2);
		else if (property.equals(ExchangeTable.UNIT))
			c = compareByUnits(e1, e2);
		else if (property.equals(ExchangeTable.AMOUNT))
			c = compareByAmounts(e1, e2);
		else if (property.equals(ExchangeTable.UNCERTAINTY))
			c = compareByUncertainty(e1, e2);
		else if (property.equals(ExchangeTable.CATEGORY))
			c = compareByCategory(e1, e2);
		return ascending ? c : -1 * c;
	}

	private int compareByCategory(Exchange e1, Exchange e2) {
		try {
			String cat1 = CategoryPath.getShort(e1.getFlow().getCategory());
			String cat2 = CategoryPath.getShort(e2.getFlow().getCategory());
			return Strings.compare(cat1, cat2);
		} catch (Exception e) {
			log.error("Comparing category paths failed", e);
			return 0;
		}
	}

	private int compareByFlows(Exchange e1, Exchange e2) {
		Flow flow1 = e1.getFlow();
		Flow flow2 = e2.getFlow();
		if (flow1 == null || flow2 == null)
			return 0;
		return Strings.compare(flow1.getName(), flow2.getName());
	}

	private int compareByProperties(Exchange e1, Exchange e2) {
		FlowProperty prop1 = e1.getFlowPropertyFactor().getFlowProperty();
		FlowProperty prop2 = e2.getFlowPropertyFactor().getFlowProperty();
		if (prop1 == null || prop2 == null)
			return 0;
		return Strings.compare(prop1.getName(), prop2.getName());
	}

	private int compareByUnits(Exchange e1, Exchange e2) {
		Unit unit1 = e1.getUnit();
		Unit unit2 = e2.getUnit();
		if (unit1 == null || unit2 == null)
			return 0;
		return Strings.compare(unit1.getName(), unit2.getName());
	}

	private int compareByAmounts(Exchange e1, Exchange e2) {
		double val1 = e1.getResultingAmount().getValue();
		double val2 = e2.getResultingAmount().getValue();
		return Double.compare(val1, val2);
	}

	private int compareByUncertainty(Exchange e1, Exchange e2) {
		String val1 = Labels.uncertaintyType(e1.getDistributionType());
		String val2 = Labels.uncertaintyType(e2.getDistributionType());
		return Strings.compare(val1, val2);
	}

}