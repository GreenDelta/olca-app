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
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ProjectResult;

class SingleScoreSection extends LabelProvider implements TableSection {

	private final ProjectResultData data;
	private final ProjectResult result;
	private final NwSetTable factors;
	private final String unit;
	private final double absMax;

	private ContributionImage image;

	private SingleScoreSection(ProjectResultData data) {
		this.data = data;
		this.result = data.result();
		this.factors = data.nwFactors();
		this.unit = data.weightedScoreUnit();
		absMax = result.getVariants().stream()
			.mapToDouble(this::singleScoreOf)
			.map(Math::abs)
			.max()
			.orElse(0);
	}

	static SingleScoreSection of(ProjectResultData data) {
		return new SingleScoreSection(data);
	}

	private double factorOf(ImpactDescriptor impact) {
		if (impact == null)
			return 0;
		var factor = factors.getWeightingFactor(impact);
		if (!factors.hasNormalization())
			return factor;
		var nf = factors.getNormalizationFactor(impact);
		return nf != 0
			? factor / nf
			: 0;
	}

	private double singleScoreOf(ProjectVariant variant) {
		return data.items()
			.impacts()
			.stream()
			.mapToDouble(i ->
				factorOf(i) * result.getTotalImpactResult(variant, i))
			.sum();
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
		var title = "Single score results";
		if (unit != null) {
			title += " [" + unit + "]";
		}
		var section = UI.section(body, tk, title);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp, M.Variant, "");
		image = contributionImage(table);
		Tables.bindColumnWidths(table, 0.6, 0.3);
		table.setLabelProvider(this);
		table.setInput(data.variants());
		section.setExpanded(false);
		Viewers.sortByLabels(table, this, 0);
		Viewers.sortByDouble(table, this, 1);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (col == 0)
			return Images.get(ModelType.PRODUCT_SYSTEM);
		if (!(obj instanceof ProjectVariant)
				|| col != 1
				|| absMax == 0)
			return null;
		var variant = (ProjectVariant) obj;
		var score = singleScoreOf(variant);
		return image.get(score / absMax);
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof ProjectVariant))
			return null;
		var variant = (ProjectVariant) obj;
		return col == 0
			? variant.name
			: Numbers.format(singleScoreOf(variant));
	}
}
