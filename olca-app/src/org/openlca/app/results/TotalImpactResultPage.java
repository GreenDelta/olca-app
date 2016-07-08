package org.openlca.app.results;

import java.util.function.Function;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUIHelper;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ImpactResult;
import org.openlca.core.results.SimpleResultProvider;

public class TotalImpactResultPage extends FormPage {

	private final String IMPACT_CATEGORY = M.ImpactCategory;
	private final String RESULT = M.Result;
	private final String REFERENCE_UNIT = M.ReferenceUnit;

	private FormToolkit toolkit;
	private SimpleResultProvider<?> result;
	private DQResult dqResult;

	public TotalImpactResultPage(FormEditor editor, SimpleResultProvider<?> result, DQResult dqResult) {
		super(editor, "ImpactResultPage", M.LCIAResult);
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.LCIAResult);
		toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		TableViewer impactViewer = createSectionAndViewer(body);
		form.reflow(true);
		impactViewer.setInput(result.getImpactDescriptors());
	}

	private TableViewer createSectionAndViewer(Composite parent) {
		Section section = UI.section(parent, toolkit, M.LCIAResult);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);
		String[] columns = { IMPACT_CATEGORY, RESULT, REFERENCE_UNIT };
		if (DQUIHelper.displayExchangeQuality(dqResult)) {
			columns = DQUIHelper.appendTableHeaders(columns, dqResult.exchangeSystem);
		}
		TableViewer viewer = Tables.createViewer(composite, columns);
		Label label = new Label();
		viewer.setLabelProvider(label);
		createColumnSorters(viewer, label);
		double[] widths = { 0.50, 0.30, 0.2 };
		if (DQUIHelper.displayExchangeQuality(dqResult)) {
			widths = DQUIHelper.adjustTableWidths(widths, dqResult.exchangeSystem);
		}
		Tables.bindColumnWidths(viewer.getTable(), widths);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
		return viewer;
	}

	private void createColumnSorters(TableViewer viewer, Label label) {
		Viewers.sortByLabels(viewer, label, 0, 2);
		Function<ImpactCategoryDescriptor, Double> amountFn = (d) -> {
			ImpactResult r = result.getTotalImpactResult(d);
			return r == null ? 0 : r.value;
		};
		Viewers.sortByDouble(viewer, amountFn, 1);
	}

	private class Label extends BaseLabelProvider implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			return Images.get(ModelType.IMPACT_CATEGORY);
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ImpactCategoryDescriptor))
				return null;
			ImpactCategoryDescriptor impactCategory = (ImpactCategoryDescriptor) element;
			switch (col) {
			case 0:
				return impactCategory.getName();
			case 1:
				double val = result.getTotalImpactResult(impactCategory).value;
				return Numbers.format(val);
			case 2:
				return impactCategory.getReferenceUnit();
			default:
				int pos = col - 3;
				int[] quality = dqResult.getImpactQuality(impactCategory.getId());
				return DQUIHelper.getLabel(pos, quality);
			}
		}

		@Override
		public Color getBackground(Object element, int col) {
			if (!(element instanceof ImpactCategoryDescriptor))
				return null;
			if (col < 3)
				return null;
			ImpactCategoryDescriptor impactCategory = (ImpactCategoryDescriptor) element;
			int pos = col - 3; // column 3 is the first dq column
			int[] quality = dqResult.getImpactQuality(impactCategory.getId());
			if (quality == null)
				return null;
			return DQUIHelper.getColor(quality[pos], dqResult.exchangeSystem.getScoreCount());
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			return null;
		}

	}

}
