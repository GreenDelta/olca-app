package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.TagResult;

public class TagResultPage extends FormPage {

	private final ResultEditor<?> editor;
	private final List<TagResult> tagResults;
	private TableViewer table;

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

		table = Tables.createViewer(comp, "Tag", "Contribution");
		table.setLabelProvider(new TagItemLabel());
		Tables.bindColumnWidths(table, 0.5, 0.5);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));

		form.reflow(true);

		App.runWithProgress("Calculate tag results", () -> {
			tagResults.clear();
			tagResults.addAll(TagResult.allOf(editor.result));
		}, selector::initWithEvent);
	}

	private record TagItem(
		String tag,
		double result,
		String unit,
		double share,
		boolean isTotal) {
	}

	private class SelectionHandler implements ResultItemSelector.SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			fillTable(Labels.refUnit(flow),
				editor.result.getTotalFlowResult(flow),
				tagResult -> tagResult.inventoryResultOf(flow).value());
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {
			fillTable(impact.referenceUnit,
				editor.result.getTotalImpactResult(impact),
				tagResult -> tagResult.impactResultOf(impact).value());
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
			if (table == null)
				return;

			var items = new ArrayList<TagItem>();
			items.add(new TagItem("Total", total, unit, 1, true));

			for (var tagResult : tagResults) {
				var value = result.applyAsDouble(tagResult);
				var share = total == 0
					? 0
					: value / Math.abs(total);
				items.add(new TagItem(tagResult.tag(), value, unit, share, false));
			}

			items.sort((item1, item2) -> {
				if (item1.isTotal)
					return 1;
				if (item2.isTotal)
					return -1;
				return Double.compare(item2.result, item1.result);
			});

			table.setInput(items);
		}
	}

	private static class TagItemLabel extends LabelProvider
		implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof TagItem item))
				return null;
			return col == 1
				? image.get(item.share)
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof TagItem item))
				return null;
			return col == 0
				? item.tag
				: Numbers.format(item.result) + " " + item.unit;
		}
	}
}
