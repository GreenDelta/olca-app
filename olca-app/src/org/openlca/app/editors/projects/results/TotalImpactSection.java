package org.openlca.app.editors.projects.results;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Strings;

class TotalImpactSection {

	private final ProjectResult result;
	private final ProjectVariant[] variants;

	private TotalImpactSection(ProjectResult result) {
		this.result = Objects.requireNonNull(result);
		variants = result.getVariants()
			.stream()
			.sorted((v1, v2) -> Strings.compare(v1.name, v2.name))
			.toArray(ProjectVariant[]::new);
	}

	static TotalImpactSection of(ProjectResult result) {
		return new TotalImpactSection(result);
	}

	void renderOn(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, M.ImpactAssessmentResults);
		Actions.bind(section, Actions.create(
			"Copy to clipboard", Icon.COPY.descriptor(), () -> {}));
		var comp = UI.sectionClient(section, tk, 1);

		// create the column headers
		var columnHeaders = new String[variants.length + 2];
		columnHeaders[0] = M.ImpactCategories;
		columnHeaders[1] = M.Unit;
		int[] sortIndices = new int[variants.length];
		for (int i = 0; i < variants.length; i++) {
			columnHeaders[i + 2] = variants[i].name;
			sortIndices[i] = i + 2;
		}

		// configure the table
		var table = Tables.createViewer(comp, columnHeaders);
		var label = new TableLabel();
		table.setLabelProvider(label);
		Viewers.sortByLabels(table, label, 0, 2);
		Viewers.sortByDouble(table, label, sortIndices);
		Tables.bindColumnWidths(table, columnWidths());

		// set the table input
		table.setInput(result.getImpacts()
			.stream()
			.sorted((i1, i2) -> Strings.compare(i1.name, i2.name))
			.map(Row::new)
			.collect(Collectors.toList()));
	}

	private double[] columnWidths() {
		var widths = new double[2 + variants.length];
		widths[0] = 0.2;
		widths[1] = 0.2;
		double other = variants.length < 4
			? 0.2
			: 0.6 / variants.length;
		Arrays.fill(widths, 2, widths.length, other);
		return widths;
	}

	private class Row {
		private final String impact;
		private final String unit;
		private final double[] results;
		private final double[] shares;

		Row(ImpactDescriptor impact) {
			this.impact = Labels.name(impact);
			this.unit = impact.referenceUnit;

			results = new double[variants.length];
			for (int i = 0; i < variants.length; i++) {
				results[i] = result.getTotalImpactResult(
					variants[i], impact);
			}

			// calculate the result shares
			shares = new double[variants.length];
			double absMax = 0;
			for (var r : results) {
				absMax = Math.max(absMax, Math.abs(r));
			}
			if (absMax > 0) {
				for (int i = 0; i < results.length; i++) {
					shares[i] = results[i] / absMax;
				}
			}
		}

		double resultOf(int column) {
			var idx = column - 2;
			return idx < 0 || idx >= variants.length
				? 0
				: results[idx];
		}

		double shareOf(int column) {
			var idx = column - 2;
			return idx < 0 || idx >= variants.length
				? 0
				: shares[idx];
		}

	}

	private static class TableLabel extends LabelProvider
		implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Row))
				return null;
			var row = (Row) obj;
			return switch (col) {
				case 0 -> Images.get(ModelType.IMPACT_CATEGORY);
				case 1 -> Images.get(ModelType.UNIT);
				default -> image.getForTable(row.shareOf(col));
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Row))
				return null;
			var row = (Row) obj;
			return switch (col) {
				case 0 -> row.impact;
				case 1 -> row.unit;
				default -> Numbers.format(row.resultOf(col));
			};
		}
	}

}
