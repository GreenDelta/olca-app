package org.openlca.app.results.requirements;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.DQLabelProvider;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;

class LabelProvider extends DQLabelProvider {

	private final ContributionImage cimg = new ContributionImage();
	private final DQResult dqResult;
	private final Costs costs;
	private final String currency;

	public LabelProvider(DQResult dqResult, Costs costs) {
		super(dqResult,
			dqResult == null ? null : dqResult.setup.processSystem,
			costs == Costs.NONE ? 4 : 5);
		this.dqResult = dqResult;
		this.costs = costs;
		var currency = costs != null && costs != Costs.NONE
			? new CurrencyDao(Database.get()).getReferenceCurrency()
			: null;
		this.currency = currency == null
			? "?"
			: currency.code;
	}

	@Override
	public void dispose() {
		cimg.dispose();
		super.dispose();
	}

	@Override
	public Image getImage(Object obj, int col) {
		if (!(obj instanceof Item))
			return null;
		Item item = (Item) obj;
		switch (col) {

			// process, sub-system, or category icon
			case 0:
				if (item.isCategory())
					return Images.getForCategory(ModelType.PROCESS);
				if (item.isChild()) {
					var product = item.asChild().product;
					return product == null
						? Images.get(ModelType.PROCESS)
					    : Images.get(product.provider());
				}
				if (item.isProvider()) {
					var provider = item.asProvider().product;
					return provider == null
						? Images.get(ModelType.PROCESS)
						: Images.get(provider.provider());
				}
				return null;

			// flow icon
			case 1:
				if (item.isCategory() || item.isChild())
					return null;
				return item.asProvider().hasWasteFlow()
					? Images.get(FlowType.WASTE_FLOW)
					: Images.get(FlowType.PRODUCT_FLOW);

			// amount share
			case 2:
				return item.isChild()
					? cimg.get(item.asChild().amountShare)
					: null;

			// costs share
			case 4:
				return costs == Costs.NONE || !item.isProvider()
					? null
					: cimg.get(item.asProvider().costShare);

			default:
				return null;
		}
	}

	@Override
	public String getText(Object obj, int col) {
		if (!(obj instanceof Item))
			return null;
		Item item = (Item) obj;
		FlowDescriptor flow = flowOf(item);
		switch (col) {

			case 0:
				return item.name();

			// flow name
			case 1:
				return !item.isProvider() || flow == null
					? null
					: Labels.name(flow);

			// amount
			case 2:
				if (item.isProvider()) {
					return Numbers.format(item.asProvider().amount);
				}
				if (item.isChild()) {
					return Numbers.format(item.asChild().amount);
				}
				return null;

			// unit
			case 3:
				return flow == null
					? null
					: Labels.refUnit(flow);

			// cost values
			case 4:
				if (!item.isProvider())
					return null;
				var val = item.asProvider().costValue;
				return Numbers.decimalFormat(val, 2) + " " + currency;

			default:
				return null;
		}
	}

	private FlowDescriptor flowOf(Item item) {
		if (item == null)
			return null;
		if (item.isProvider())
			return item.asProvider().product.flow();
		if (item.isChild())
			return item.asChild().parent.product.flow();
		return null;
	}

	@Override
	protected int[] getQuality(Object obj) {
		if (!(obj instanceof Item))
			return null;
		Item item = (Item) obj;
		return item.isProvider()
			? dqResult.get(item.asProvider().product)
			: null;
	}
}
