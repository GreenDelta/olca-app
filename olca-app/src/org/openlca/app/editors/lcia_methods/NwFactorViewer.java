package org.openlca.app.editors.lcia_methods;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.editors.lcia_methods.NwFactorViewer.Wrapper;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;

class NwFactorViewer extends AbstractTableViewer<Wrapper> {

	private interface LABEL {
		String IMPACT_CATEGORY = Messages.ImpactCategory;
		String NORMALIZATION = Messages.NormalizationFactor;
		String WEIGHTING = Messages.WeightingFactor;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.IMPACT_CATEGORY,
			LABEL.NORMALIZATION, LABEL.WEIGHTING };

	private final ImpactMethod method;
	private NwSet set;

	public NwFactorViewer(Composite parent, ImpactMethod impactMethod) {
		super(parent);
		this.method = impactMethod;
		getCellModifySupport().bind(LABEL.NORMALIZATION,
				new NormalizationModifier());
		getCellModifySupport().bind(LABEL.WEIGHTING, new WeightingModifier());
	}

	public void setInput(NwSet set) {
		this.set = set;
		if (set == null)
			setInput(new Wrapper[0]);
		else {
			Wrapper[] wrapper = new Wrapper[method.getImpactCategories().size()];
			for (int i = 0; i < wrapper.length; i++) {
				ImpactCategory category = method.getImpactCategories().get(i);
				wrapper[i] = new Wrapper(category);
				NwFactor f = set.getFactor(category);
				wrapper[i].factor = f;
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
		private NwFactor factor;

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
				if (wrapper.factor.getNormalisationFactor() == null)
					return "-";
				return Double.toString(wrapper.factor.getNormalisationFactor());
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
			if (element.factor.getNormalisationFactor() == null)
				return "-";
			return Double.toString(element.factor.getNormalisationFactor());
		}

		@Override
		protected void setText(Wrapper element, String text) {
			try {
				double factor = Double.parseDouble(text);
				if (element.factor == null) {
					element.factor = new NwFactor();
					element.factor.setImpactCategory(element.category);
					set.getFactors().add(element.factor);
				}
				if (element.factor.getNormalisationFactor() == null
						|| element.factor.getNormalisationFactor() != factor) {
					element.factor.setNormalisationFactor(factor);
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
					element.factor = new NwFactor();
					element.factor.setImpactCategory(element.category);
					set.getFactors().add(element.factor);
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
