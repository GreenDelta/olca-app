package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.util.Strings;

/**
 * A helper class for the selection of units and flow properties in the
 * input-output tables.
 */
class UnitItem implements Comparable<UnitItem> {

	final Unit unit;
	final FlowPropertyFactor factor;
	final Exchange exchange;

	UnitItem(Unit unit, FlowPropertyFactor factor, Exchange exchange) {
		this.unit = unit;
		this.factor = factor;
		this.exchange = exchange;
	}

	@Override
	public String toString() {
		String name = unit.name;
		if (name == null)
			return "?";
		Flow f = exchange.flow;
		if (f.flowPropertyFactors.size() == 1)
			return name;
		FlowProperty fp = factor.flowProperty;
		return name + " - " + fp.name + "";
	}

	@Override
	public int compareTo(UnitItem other) {
		if (other == null)
			return 1;
		FlowProperty thisFp = factor.flowProperty;
		FlowProperty otherFp = other.factor.flowProperty;
		FlowProperty exchFp = exchange.flowPropertyFactor.flowProperty;
		if (Objects.equals(thisFp, otherFp))
			return Strings.compare(this.toString(), other.toString());
		if (Objects.equals(thisFp, exchFp))
			return -1;
		if (Objects.equals(otherFp, exchFp))
			return 1;
		else
			return Strings.compare(thisFp.name, otherFp.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (!(obj instanceof UnitItem))
			return false;
		UnitItem other = (UnitItem) obj;
		return Objects.equals(this.unit, other.unit)
				&& Objects.equals(this.factor, other.factor)
				&& Objects.equals(this.exchange, other.exchange);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(toString());
	}

}
