package org.openlca.app.results.impacts;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.DQLabelProvider;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.util.Strings;

class ImpactTreeLabel extends DQLabelProvider {

	private final ContributionImage img = new ContributionImage();

	ImpactTreeLabel(DQResult dqResult) {
		super(dqResult, dqResult != null
				? dqResult.setup.exchangeSystem
				: null, 5);
	}

	@Override
	public void dispose() {
		img.dispose();
		super.dispose();
	}

	@Override
	public Image getImage(Object obj, int col) {
		if (!(obj instanceof Item item))
			return null;
		if (col == 0) {
			if (item.isRoot())
				return Images.get(item.impact());
			return item.isTechItem()
					? Images.get(item.techFlow())
					: Images.get(item.enviFlow());
		}
		return col == 4 && !item.isRoot()
				? img.get(item.contributionShare())
				: null;
	}

	@Override
	public String getText(Object obj, int col) {
		if (!(obj instanceof Item item))
			return null;
		return switch (col) {
			case 0 -> nameOf(item);
			case 1 -> categoryOf(item);
			case 2 -> inventoryResultOf(item);
			case 3 -> impactFactorOf(item);
			case 4 -> format(item.impactResult(), item.impact().referenceUnit);
			default -> null;
		};
	}

	private String nameOf(Item item) {
		if (item.isRoot())
			return Labels.name(item.impact());
		return item.isTechItem()
				? Labels.name(item.techFlow())
				: Labels.name(item.enviFlow());
	}

	private String categoryOf(Item item) {
		if (item.isRoot())
			return Labels.category(item.impact());
		return item.isTechItem()
				? Labels.category(item.techFlow())
				: Labels.category(item.enviFlow());
	}

	private String inventoryResultOf(Item item) {
		if (item.isEnviItem()) {
			var unit = Labels.refUnit(item.enviFlow());
			return format(item.inventoryResult(), unit);
		}
		if (item.isTechItem() && item.isLeaf()) {
			var unit = Labels.refUnit(item.parent().enviFlow());
			return format(item.inventoryResult(), unit);
		}
		return null;
	}

	private String impactFactorOf(Item item) {
		if (!item.isEnviItem())
			return null;
		var flowUnit = Labels.refUnit(item.enviFlow());
		var impactUnit = item.impact().referenceUnit;
		var unit = Strings.notEmpty(impactUnit)
				? impactUnit + "/" + flowUnit
				: "1/" + flowUnit;
		return format(item.impactFactor(), unit);
	}

	private String format(double amount, String unit) {
		return Strings.notEmpty(unit)
				? Numbers.format(amount) + " " + unit
				: Numbers.format(amount);
	}

	@Override
	protected int[] getQuality(Object obj) {
		if (dqResult == null)
			return null;
		if (!(obj instanceof Item item))
			return null;
		if (item.isRoot())
			return dqResult.get(item.impact());
		if (item.isInnerNode())
			return item.isTechItem()
					? dqResult.get(item.impact(), item.techFlow())
					: dqResult.get(item.impact(), item.enviFlow());
		return item.isTechItem()
				? dqResult.get(item.techFlow(), item.parent().enviFlow())
				: dqResult.get(item.parent().techFlow(), item.enviFlow());
	}
}
