package org.openlca.app.results.comparison.display;

import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

public class Result {

	private Contribution<CategorizedDescriptor> contribution;
	static ColorCellCriteria criteria;

	public Result(Contribution<CategorizedDescriptor> item) {
		contribution = item;
	}

	public double getAmount() {
		return contribution.amount;
	}

	public double getValue() {
		try {
			switch (criteria) {
			case CATEGORY:
				return contribution.item.category;
			case LOCATION:
				return ((ProcessDescriptor) contribution.item).location;
			default:
				return contribution.amount;
			}
		} catch (ClassCastException | NullPointerException e) {
			return 0;
		}
	}

	public Contribution<CategorizedDescriptor> getContribution() {
		return contribution;
	}

	@Override
	public String toString() {
		return contribution.item.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Result)) {
			return false;
		}
		Result r = (Result) o;
		boolean comparison;
		try {
			switch (criteria) {
			case CATEGORY:
				comparison = ((CategorizedDescriptor) r.contribution.item).category
						.equals(((CategorizedDescriptor) contribution.item).category);
				break;
			case LOCATION:
				comparison = ((ProcessDescriptor) r.contribution.item).location
						.equals(((ProcessDescriptor) contribution.item).location);
				break;
			default:
				comparison = contribution.amount == r.contribution.amount;
			}
			return comparison;
		} catch (NullPointerException e) {
			return false;
		}
	}
}
