package org.openlca.app.editors.projects.results;

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

class NwSection extends LabelProvider implements TableSection {

	private enum Type {
		NORMALIZATION,
		WEIGHTING
	}

	private final Type type;
	private final ProjectResultData data;
	private final String unit;
	private final double absMax;

	private ContributionImage image;

	private NwSection(Type type, ProjectResultData data) {
		this.type = type;
		this.data = data;
		this.unit = data.weightedScoreUnit();
		double _absMax = 0;
		for (var variant : data.variants()) {
			for (var impact : data.items().impacts()) {
				_absMax = Math.max(_absMax, Math.abs(resultOf(impact, variant)));
			}
		}
		this.absMax = _absMax;
	}

	static NwSection forNormalization(ProjectResultData data) {
		return new NwSection(Type.NORMALIZATION, data);
	}

	static NwSection forWeighting(ProjectResultData data) {
		return new NwSection(Type.WEIGHTING, data);
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
		var title = type == Type.NORMALIZATION
			? "Normalized results"
			: "Weighted results";
		var section = UI.section(body, tk,
			title + (unit != null ? " [" + unit + "]" : ""));
		var comp = UI.sectionClient(section, tk, 1);

		var variants = data.variants();
		var columnTitles = new String[variants.length + 1];
		var columnWidths = new double[variants.length + 1];
		var sortIndices = new int[variants.length];
		columnTitles[0] = M.ImpactCategory;
		columnWidths[0] = 0.25;
		for (int i = 0; i < variants.length; i++) {
			columnTitles[i + 1] = variants[i].name;
			columnWidths[i + 1] = 0.745 / variants.length;
			sortIndices[i] = i + 1;
		}

		var table = Tables.createViewer(comp, columnTitles);
		image = contributionImage(table);
		Tables.bindColumnWidths(table, columnWidths);
		table.setLabelProvider(this);
		table.setInput(data.items().impacts());
		section.setExpanded(false);
		Viewers.sortByLabels(table, this, 0);
		Viewers.sortByDouble(table, this, sortIndices);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));
	}

	private double resultOf(ImpactDescriptor impact, ProjectVariant variant) {
		var factors = data.nwFactors();
		double factor;
		if (type == Type.NORMALIZATION) {
			var nf = factors.getNormalizationFactor(impact);
			factor = nf == 0 ? 0 : 1 / nf;
		} else if (factors.hasNormalization()) {
			var nf = factors.getNormalizationFactor(impact);
			factor = nf != 0
				? factors.getWeightingFactor(impact) / nf
				: 0;
		} else {
			factor = factors.getWeightingFactor(impact);
		}
		return factor * data.result().getTotalImpactResult(variant, impact);
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (col == 0)
			return Images.get(ModelType.IMPACT_CATEGORY);
		if (!(obj instanceof ImpactDescriptor)
				|| col > data.variants().length
				|| col < 0)
			return null;
		var impact = (ImpactDescriptor) obj;
		var variant = data.variants()[col - 1];
		double result = resultOf(impact, variant);
		return absMax == 0
			? image.get(0)
			: image.get(result / absMax);
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof ImpactDescriptor))
			return null;
		var impact = (ImpactDescriptor) obj;
		if (col == 0)
			return Labels.name(impact);
		if (col > data.variants().length || col < 0)
			return null;
		var variant = data.variants()[col - 1];
		double result = resultOf(impact, variant);
		return Numbers.format(result);
	}
}
