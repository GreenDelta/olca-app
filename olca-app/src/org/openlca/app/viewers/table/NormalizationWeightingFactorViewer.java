package org.openlca.app.viewers.table;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.viewers.table.NormalizationWeightingFactorViewer.Wrapper;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NormalizationWeightingFactor;
import org.openlca.core.model.NormalizationWeightingSet;

public class NormalizationWeightingFactorViewer extends
		AbstractTableViewer<Wrapper> {

	private interface LABEL {
		String IMPACT_CATEGORY = Messages.Common_ImpactCategory;
		String NORMALIZATION = Messages.Methods_NormalizationFactor;
		String WEIGHTING = Messages.Methods_WeightingFactor;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.IMPACT_CATEGORY,
			LABEL.NORMALIZATION, LABEL.WEIGHTING };

	private final ImpactMethod method;
	private NormalizationWeightingSet set;

	public NormalizationWeightingFactorViewer(Composite parent,
			ImpactMethod impactMethod) {
		super(parent);
		this.method = impactMethod;
		getCellModifySupport().support(LABEL.NORMALIZATION,
				new NormalizationModifier());
		getCellModifySupport()
				.support(LABEL.WEIGHTING, new WeightingModifier());
	}

	public void setInput(NormalizationWeightingSet set) {
		this.set = set;
		if (set == null)
			setInput(new Wrapper[0]);
		else {
			Wrapper[] wrapper = new Wrapper[method.getImpactCategories().size()];
			for (int i = 0; i < wrapper.length; i++) {
				ImpactCategory category = method.getImpactCategories().get(i);
				wrapper[i] = new Wrapper(category);
				wrapper[i].factor = set.getFactor(category);
			}
			setInput(wrapper);
		}
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new SetLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	public class Wrapper {

		private ImpactCategory category;
		private NormalizationWeightingFactor factor;

		private Wrapper(ImpactCategory category) {
			this.category = category;
		}

	}

	private class SetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Wrapper))
				return null;
			Wrapper wrapper = (Wrapper) element;
			switch (columnIndex) {
			case 0:
				return wrapper.category.getName();
			case 1:
				if (wrapper.factor == null)
					return "-";
				if (wrapper.factor.getNormalizationFactor() == null)
					return "-";
				return Double.toString(wrapper.factor.getNormalizationFactor());
			case 2:
				if (wrapper.factor == null)
					return "-";
				if (wrapper.factor.getWeightingFactor() == null)
					return "-";
				return Double.toString(wrapper.factor.getWeightingFactor());
			default:
				return null;
			}
		}
	}

	private class NormalizationModifier extends TextCellModifier<Wrapper> {

		@Override
		protected String getText(Wrapper element) {
			if (element.factor == null)
				return "-";
			if (element.factor.getNormalizationFactor() == null)
				return "-";
			return Double.toString(element.factor.getNormalizationFactor());
		}

		@Override
		protected void setText(Wrapper element, String text) {
			try {
				double factor = Double.parseDouble(text);
				if (element.factor == null) {
					element.factor = new NormalizationWeightingFactor();
					element.factor
							.setImpactCategoryId(element.category.getId());
					set.getNormalizationWeightingFactors().add(element.factor);
				}
				if (element.factor.getNormalizationFactor() == null
						|| element.factor.getNormalizationFactor() != factor) {
					element.factor.setNormalizationFactor(factor);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {

			}
		}
	}

	private class WeightingModifier extends TextCellModifier<Wrapper> {

		@Override
		protected String getText(Wrapper element) {
			if (element.factor == null)
				return "-";
			if (element.factor.getWeightingFactor() == null)
				return "-";
			return Double.toString(element.factor.getWeightingFactor());
		}

		@Override
		protected void setText(Wrapper element, String text) {
			try {
				double factor = Double.parseDouble(text);
				if (element.factor == null) {
					element.factor = new NormalizationWeightingFactor();
					element.factor
							.setImpactCategoryId(element.category.getId());
					set.getNormalizationWeightingFactors().add(element.factor);
				}
				if (element.factor.getWeightingFactor() == null
						|| element.factor.getWeightingFactor() != factor) {
					element.factor.setWeightingFactor(factor);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {

			}
		}
	}

}
