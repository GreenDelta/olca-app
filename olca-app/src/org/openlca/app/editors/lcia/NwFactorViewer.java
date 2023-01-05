package org.openlca.app.editors.lcia;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.lcia.NwFactorViewer.Item;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;

class NwFactorViewer extends AbstractTableViewer<Item> {

	private static final String IMPACT_CATEGORY = M.ImpactCategory;
	private static final String NORMALIZATION = M.NormalizationValue;
	private static final String WEIGHTING = M.WeightingFactor;
	private static final String COMMENT = "";

	private NwSet set;
	private final ImpactMethodEditor editor;

	public NwFactorViewer(Composite parent, ImpactMethodEditor editor) {
		super(parent);
		this.editor = editor;
		getModifySupport().bind(NORMALIZATION, new NormalizationModifier());
		getModifySupport().bind(WEIGHTING, new WeightingModifier());
		getModifySupport().bind("", new CommentDialogModifier<>(editor.getComments(),
				w -> CommentPaths.get(set, w.factor)));
		getViewer().getTable().getColumns()[1].setAlignment(SWT.RIGHT);
		getViewer().getTable().getColumns()[2].setAlignment(SWT.RIGHT);
	}

	public void setInput(NwSet set) {
		this.set = set;
		if (set == null)
			setInput(new Item[0]);
		else {
			var categories = editor.getModel().impactCategories;
			var wrappers = new Item[categories.size()];
			for (int i = 0; i < wrappers.length; i++) {
				var category = categories.get(i);
				wrappers[i] = new Item(category);
				wrappers[i].factor = set.getFactor(category);
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
		return new String[]{IMPACT_CATEGORY, NORMALIZATION, WEIGHTING, COMMENT};
	}

	static class Item {

		private final ImpactCategory category;
		private NwFactor factor;

		private Item(ImpactCategory category) {
			this.category = category;
		}

	}

	private class SetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			if (!(element instanceof Item item))
				return null;
			if (column == 0)
				return Images.get(ModelType.IMPACT_CATEGORY);
			if (column == 3 && item.factor != null)
				return Images.get(editor.getComments(), CommentPaths.get(set, item.factor));
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Item item))
				return null;
			return switch (columnIndex) {
				case 0 -> item.category.name;
				case 1 -> item.factor != null && item.factor.normalisationFactor != null
						? Double.toString(item.factor.normalisationFactor)
						: "-";
				case 2 -> item.factor != null && item.factor.weightingFactor != null
						? Double.toString(item.factor.weightingFactor)
						: "-";
				default -> null;
			};
		}
	}

	private class NormalizationModifier extends TextCellModifier<Item> {

		@Override
		protected String getText(Item element) {
			if (element.factor == null)
				return "-";
			if (element.factor.normalisationFactor == null)
				return "-";
			return Double.toString(element.factor.normalisationFactor);
		}

		@Override
		protected void setText(Item item, String text) {
			try {
				double factor = Double.parseDouble(text);
				if (item.factor == null) {
					item.factor = new NwFactor();
					item.factor.impactCategory = item.category;
					set.factors.add(item.factor);
				}
				if (item.factor.normalisationFactor == null
						|| item.factor.normalisationFactor != factor) {
					item.factor.normalisationFactor = factor;
					editor.setDirty(true);
				}
			} catch (NumberFormatException ignored) {

			}
		}
	}

	private class WeightingModifier extends TextCellModifier<Item> {

		@Override
		protected String getText(Item element) {
			if (element.factor == null)
				return "-";
			if (element.factor.weightingFactor == null)
				return "-";
			return Double.toString(element.factor.weightingFactor);
		}

		@Override
		protected void setText(Item w, String text) {
			try {
				double factor = Double.parseDouble(text);
				if (w.factor == null) {
					w.factor = new NwFactor();
					w.factor.impactCategory = w.category;
					set.factors.add(w.factor);
				}
				if (w.factor.weightingFactor == null
						|| w.factor.weightingFactor != factor) {
					w.factor.weightingFactor = factor;
					editor.setDirty(true);
				}
			} catch (NumberFormatException ignored) {
			}
		}
	}

}
