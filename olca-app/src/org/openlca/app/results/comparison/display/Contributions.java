package org.openlca.app.results.comparison.display;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.util.Pair;

public class Contributions {

	private ArrayList<Cell> list;
	private String impactCategoryName;
	private String productSystemName;
	static ColorCellCriteria criteria;
	static Config config;
	long minProcessId = -1, maxProcessId = -1;
	double minAmount;
	long minCategory = -1, maxCategory = -1;
	long minLocation = -1, maxLocation = -1;
	private List<Contribution<CategorizedDescriptor>> contributions;

	public Contributions(List<Contribution<CategorizedDescriptor>> l, String n, String productSystemName) {
		contributions = l;
		impactCategoryName = n;
		this.productSystemName = productSystemName;
		list = new ArrayList<>();
		Result.criteria = criteria;

		minAmount = l.stream().mapToDouble(c -> c.amount).min().getAsDouble();
		for (Contribution<CategorizedDescriptor> contribution : l) {
			list.add(new Cell(contribution, minAmount, this));
		}
	}

	public Pair<Long, Long> getMinMaxProcessId() {
		if (maxProcessId == -1) {
			maxProcessId = contributions.stream().mapToLong(c -> c.item.id).max().getAsLong();
			minProcessId = contributions.stream().mapToLong(c -> c.item.id).min().getAsLong();
		}
		return new Pair<Long, Long>(minProcessId, maxProcessId);
	}

	public Pair<Long, Long> getMinMaxCategory() {
		if (maxCategory == -1) {
			maxCategory = contributions.stream().mapToLong(c -> c.item.category).max().getAsLong();
			minCategory = contributions.stream().mapToLong(c -> c.item.category).min().getAsLong();
		}
		return new Pair<Long, Long>(minCategory, maxCategory);
	}

	public Pair<Long, Long> getMinMaxLocation() {
		if (maxLocation == -1) {
			maxLocation = contributions.stream().mapToLong(c -> ((ProcessDescriptor) c.item).location).max()
					.getAsLong();
			minLocation = contributions.stream().mapToLong(c -> ((ProcessDescriptor) c.item).location).min()
					.getAsLong();
		}
		return new Pair<Long, Long>(minLocation, maxLocation);
	}

	public void updateCellsColor() {
		list.stream().forEach(c -> c.computeRGB());
	}

	public static void updateComparisonCriteria(ColorCellCriteria c) {
		criteria = c;
		Result.criteria = c;
		Cell.criteria = c;
	}

	public String getImpactCategoryName() {
		return impactCategoryName;
	}

	public String getProductSystemName() {
		return productSystemName;
	}

	public ArrayList<Cell> getList() {
		return list;
	}

	public double minProcessId() {
		return minProcessId;
	}

	public double maxProcessId() {
		return maxProcessId;
	}

	@Override
	public String toString() {
		String s = "[ ";
		for (Cell c : list) {
			var item = c.getResult().getContribution().item.toString();
			s += String.join(", ", item);
		}
		s += " ]";
		return s;
	}

	/**
	 * Ascending sort of the products results
	 */
	public void sort() {
		list.sort((r1, r2) -> {
			double a1 = r1.getResult().getContribution().amount;
			double a2 = r2.getResult().getContribution().amount;
			if (a1 == 0.0 && a2 != 0.0) {
				return -1;
			} else if (a1 != 0.0 && a2 == 0.0) {
				return 1;
			}
			if (a2 > a1) {
				return -1;
			}
			if (a1 > a2) {
				return 1;
			}
			return 0;
		});
	}
}
