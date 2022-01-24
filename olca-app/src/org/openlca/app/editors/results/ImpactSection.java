package org.openlca.app.editors.results;

import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ImpactResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

record ImpactSection(ResultEditor editor) {

	private Result result() {
		return editor.getModel();
	}

	void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, M.ImpactAssessmentResults);
		UI.gridData(section, true, false);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(
			comp, M.ImpactCategory, M.Amount, M.Unit);
		table.setLabelProvider(new ImpactLabel());
		Tables.bindColumnWidths(table, 0.35, 0.35, 0.3);
		new ModifySupport<ImpactResult>(table)
			.bind(M.Amount, new AmountModifier(editor));
		bindActions(section, table);

		var impacts = result().impactResults.stream()
			.sorted((i1, i2) -> Strings.compare(
				Labels.name(i1.indicator), Labels.name(i2.indicator)))
			.collect(Collectors.toList());
		table.setInput(impacts);
	}

	private void bindActions(Section section, TableViewer table) {

		var onAdd = Actions.onAdd(() -> {
			var result = result();
			new ModelSelector(ModelType.IMPACT_CATEGORY)
				.withFilter(d -> Util.canAddImpact(result, d))
				.onOk(ModelSelector::first)
				.ifPresent(d -> {
					if (Util.addImpact(result, d)) {
						table.setInput(result.impactResults);
						editor.setDirty();
					}
				});
		});

		var onRemove = Actions.onRemove(() -> {
			ImpactResult impact = Viewers.getFirstSelected(table);
			if (impact == null)
				return;
			var impacts = result().impactResults;
			impacts.remove(impact);
			table.setInput(impacts);
			editor.setDirty();
		});

		var onOpen = Actions.onOpen(() -> {
			ImpactResult impact = Viewers.getFirstSelected(table);
			if (impact != null) {
				App.open(impact.indicator);
			}
		});
		Tables.onDoubleClick(table, $ -> onOpen.run());

		Actions.bind(section, onAdd, onRemove);
		Actions.bind(table,
			onAdd, onRemove, onOpen, TableClipboard.onCopySelected(table));
	}

	private static class ImpactLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0
				? Images.get(ModelType.IMPACT_CATEGORY)
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactResult impact))
				return null;
			return switch (col) {
				case 0 -> Labels.name(impact.indicator);
				case 1 -> Numbers.format(impact.amount);
				case 2 -> impact.indicator == null
					? null
					: impact.indicator.referenceUnit;
				default -> null;
			};
		}
	}

	private static class AmountModifier extends DoubleCellModifier<ImpactResult> {

		private final ResultEditor editor;

		AmountModifier(ResultEditor editor) {
			this.editor = editor;
		}

		@Override
		public Double getDouble(ImpactResult impact) {
			return impact == null
				? null
				: impact.amount;
		}

		@Override
		public void setDouble(ImpactResult impact, Double value) {
			if (impact == null)
				return;
			double v = value == null ? 0 : value;
			if (v == impact.amount)
				return;
			impact.amount = v;
			editor.setDirty();
		}
	}

}
