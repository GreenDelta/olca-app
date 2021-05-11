package org.openlca.app.editors.projects.results;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Strings;

class NwSection extends LabelProvider implements ITableLabelProvider {

	private enum Type {
		NORMALIZATION,
		WEIGHTING
	}

	private final Type type;
	private final NwSetTable factors;
	private final ProjectResult result;
	private final ProjectVariant[] variants;

	private final double absMax;
	private ContributionImage image;

	private NwSection(Type type, NwSetTable factors, ProjectResult result) {
		this.type = type;
		this.factors = factors;
		this.result = result;
		this.variants = result.getVariants()
			.stream()
			.sorted((v1, v2) -> Strings.compare(v1.name, v2.name))
			.toArray(ProjectVariant[]::new);
		double _absMax = 0;
		for (var variant : variants) {
			for (var impact : result.getImpacts()) {
				_absMax = Math.max(_absMax, Math.abs(resultOf(impact, variant)));
			}
		}
		this.absMax = _absMax;
	}

	static NwSection forNormalization(NwSetTable factors, ProjectResult result) {
		return new NwSection(Type.NORMALIZATION, factors, result);
	}

	static NwSection forWeighting(NwSetTable factors, ProjectResult result) {
		return new NwSection(Type.WEIGHTING, factors, result);
	}

	void renderOn(Composite body, FormToolkit tk) {
		var title = type == Type.NORMALIZATION
			? "Normalized results"
			: "Weighted results";
		var section = UI.section(body, tk, title);
		var comp = UI.sectionClient(section, tk, 1);

		var columnTitles = new String[variants.length + 1];
		var columnWidths = new double[variants.length + 1];
		columnTitles[0] = M.ImpactCategory;
		columnWidths[0] = 0.25;
		for (int i = 0; i < variants.length; i++) {
			columnTitles[i + 1] = variants[i].name;
			columnWidths[i + 1] = 0.7 / variants.length;
		}
		var table = Tables.createViewer(comp, columnTitles);
		image = new ContributionImage();
		table.getControl().addDisposeListener($ -> image.dispose());
		Tables.bindColumnWidths(table, columnWidths);
		table.setLabelProvider(this);
		table.setInput(result.getImpacts());
		section.setExpanded(false);
	}

	private double resultOf(ImpactDescriptor impact, ProjectVariant variant) {
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
		return factor * result.getTotalImpactResult(variant, impact);
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (col == 0)
			return Images.get(ModelType.IMPACT_CATEGORY);
		if (!(obj instanceof ImpactDescriptor)
				|| col > variants.length
				|| col < 0)
			return null;
		var impact = (ImpactDescriptor) obj;
		var variant = variants[col - 1];
		double result = resultOf(impact, variant);
		return absMax == 0
			? image.getForTable(0)
			: image.getForTable(result / absMax);
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof ImpactDescriptor))
			return null;
		var impact = (ImpactDescriptor) obj;
		if (col == 0)
			return Labels.name(impact);
		if (col > variants.length || col < 0)
			return null;
		var variant = variants[col - 1];
		double result = resultOf(impact, variant);
		return Numbers.format(result);
	}
}
