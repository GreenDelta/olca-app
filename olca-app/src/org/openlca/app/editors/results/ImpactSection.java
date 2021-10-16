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
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultImpact;
import org.openlca.core.model.ResultModel;
import org.openlca.core.model.ResultOrigin;
import org.openlca.util.Strings;

record ImpactSection(ResultEditor editor) {

	private ResultModel result() {
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
		new ModifySupport<ResultImpact>(table)
			.bind(M.Amount, new AmountModifier(editor));
		bindActions(section, table);

		var impacts = result().impacts.stream()
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
						table.setInput(result.impacts);
						editor.setDirty();
					}
				});
		});

		var onRemove = Actions.onRemove(() -> {
			ResultImpact impact = Viewers.getFirstSelected(table);
			if (impact == null)
				return;
			var impacts = result().impacts;
			impacts.remove(impact);
			table.setInput(impacts);
			editor.setDirty();
		});

		var onOpen = Actions.onOpen(() -> {
			ResultImpact impact = Viewers.getFirstSelected(table);
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
			if (!(obj instanceof ResultImpact impact))
				return null;
			return switch (col) {
				case 0 -> Images.get(ModelType.IMPACT_CATEGORY);
				case 1 -> ResultEditor.iconOf(impact.origin);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ResultImpact impact))
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

	private static class AmountModifier extends DoubleCellModifier<ResultImpact> {

		private final ResultEditor editor;

		AmountModifier(ResultEditor editor) {
			this.editor = editor;
		}

		@Override
		public Double getDouble(ResultImpact impact) {
			return impact == null
				? null
				: impact.amount;
		}

		@Override
		public void setDouble(ResultImpact impact, Double value) {
			if (impact == null)
				return;
			double v = value == null ? 0 : value;
			if (v == impact.amount)
				return;
			impact.amount = v;
			impact.origin = ResultOrigin.ENTERED;
			editor.setDirty();
		}
	}

}
