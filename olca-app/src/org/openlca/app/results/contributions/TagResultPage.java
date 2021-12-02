package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.TagResult;

public class TagResultPage extends FormPage {

	private final ResultEditor<?> editor;
	private final List<TagResult> tagResults;

	public TagResultPage(ResultEditor<?> editor) {
		super(editor, "TagResultPage", "Tags");
		this.editor = editor;
		this.tagResults = new ArrayList<>();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform,
			Labels.name(editor.setup.target()),
			Images.get(editor.result));
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);

		var section = UI.section(body, tk, "Contributions by tag");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		var selector = ResultItemSelector
			.on(editor.resultItems)
			.withSelectionHandler(new SelectionHandler())
			.create(comp, tk);


		form.reflow(true);
	}

	private record TagItem(
		String tag,
		double result,
		String unit,
		double share,
		boolean isTotal) {

		static List<TagItem> allOf(EnviFlow flow) {
			var items = new ArrayList<TagItem>();
			var unit = Labels.refUnit(flow);
			double total = editor.result.getTotalFlowResult(flow).value;
			items.add(new TagItem("Total", total, unit, 100, true);

			for (var tagResult : tagResults) {
				var result = tagResult.flowResult(flow);
				var share = total == 0
					? 0
					: 100 * share / Math.abs(total);
				items.add(new TagItem(tagResult.tag(), result, unit, share, false));
			}

			items.sort((item1, item2) -> {
				if (item1.isTotal)
					return -1;
				if (item2.isTotal)
					return 1;
				return Double.compare(item2.result, item1.result);
			});

		}


	}

	private class SelectionHandler implements ResultItemSelector.SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			fillTable(Labels.refUnit(flow),
				editor.result.getTotalFlowResult(flow),
				tagResult -> tagResult.inventoryResultOf(flow).value);
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {
			fillTable(impact.referenceUnit,
				editor.result.getTotalImpactResult(impact),
				tagResult -> tagResult.impactResultOf(impact).value);
		}

		@Override
		public void onCostsSelected(CostResultDescriptor cost) {
			var total = cost.forAddedValue
				? -editor.result.totalCosts()
				: editor.result.totalCosts();
			fillTable(Labels.getReferenceCurrencyCode(),
				total,
				tagResults -> cost.forAddedValue
					? -tagResults.costs()
					: tagResults.costs());
		}

		private void fillTable(String unit, double total,
													 ToDoubleFunction<TagResult> result) {

			var items = new ArrayList<TagItem>();
			items.add(new TagItem("Total", total, unit, 100, true);

			for (var tagResult : tagResults) {
				var value = result.applyAsDouble(tagResult);
				var share = total == 0
					? 0
					: 100 * value / Math.abs(total);
				items.add(new TagItem(tagResult.tag(), value, unit, share, false));
			}

			items.sort((item1, item2) -> {
				if (item1.isTotal)
					return -1;
				if (item2.isTotal)
					return 1;
				return Double.compare(item2.result, item1.result);
			});

		}
	}
}
