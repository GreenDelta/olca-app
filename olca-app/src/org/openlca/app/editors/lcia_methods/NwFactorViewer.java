package org.openlca.app.editors.lcia_methods;

import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.editors.lcia_methods.NwFactorViewer.Wrapper;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;

class NwFactorViewer extends AbstractTableViewer<Wrapper> {

	private static final String IMPACT_CATEGORY = M.ImpactCategory;
	private static final String NORMALIZATION = M.NormalizationFactor;
	private static final String WEIGHTING = M.WeightingFactor;

	private NwSet set;
	private final ImpactMethodEditor editor;

	public NwFactorViewer(Composite parent, ImpactMethodEditor editor) {
		super(parent);
		this.editor = editor;
		getModifySupport().bind(NORMALIZATION, new NormalizationModifier());
		getModifySupport().bind(WEIGHTING, new WeightingModifier());
		getViewer().getTable().getColumns()[1].setAlignment(SWT.RIGHT);
		getViewer().getTable().getColumns()[2].setAlignment(SWT.RIGHT);
	}

	public void setInput(NwSet set) {
		this.set = set;
		if (set == null)
			setInput(new Wrapper[0]);
		else {
			List<ImpactCategory> categories = editor.getModel()
					.getImpactCategories();
			Wrapper[] wrappers = new Wrapper[categories.size()];
			for (int i = 0; i < wrappers.length; i++) {
				ImpactCategory category = categories.get(i);
				wrappers[i] = new Wrapper(category);
				NwFactor f = set.getFactor(category);
				wrappers[i].factor = f;
			}
			setInput(wrappers);
		}
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new SetLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { IMPACT_CATEGORY, NORMALIZATION, WEIGHTING };
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
			if (column == 0)
				return Images.get(ModelType.IMPACT_CATEGORY); 
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
					editor.setDirty(true);
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
					editor.setDirty(true);
				}
			} catch (NumberFormatException e) {

			}
		}
	}

}
