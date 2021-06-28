package org.openlca.app.editors.projects.results;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.editors.projects.ProjectResultData;
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

class TotalImpactSection extends LabelProvider implements TableSection {

	private final ProjectResultData data;
	private final ProjectVariant[] variants;
	private final ProjectResult result;
	private ContributionImage image;

	private TotalImpactSection(ProjectResultData data) {
		this.data = data;
		this.variants = data.variants();
		this.result = data.result();
	}

	static TotalImpactSection of(ProjectResultData data) {
		return new TotalImpactSection(data);
	}

	@Override
	public void renderOn(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, M.ImpactAssessmentResults);
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
		image = contributionImage(table);
		table.setLabelProvider(this);
		Viewers.sortByLabels(table, this, 0, 2);
		Viewers.sortByDouble(table, this, sortIndices);
		Tables.bindColumnWidths(table, columnWidths());
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));

		// set the table input
		table.setInput(data.items()
			.impacts()
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

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Row))
			return null;
		var row = (Row) obj;
		return switch (col) {
			case 0 -> Images.get(ModelType.IMPACT_CATEGORY);
			case 1 -> Images.get(ModelType.UNIT);
			default -> image.get(row.shareOf(col));
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
}
