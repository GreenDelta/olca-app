package org.openlca.app.results;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.TableColumnSorter;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.SimpleResultProvider;

import com.google.common.primitives.Doubles;

public class TotalImpactResultPage extends FormPage {

	private final String IMPACT_CATEGORY = Messages.ImpactCategory;
	private final String RESULT = Messages.Result;
	private final String REFERENCE_UNIT = Messages.ReferenceUnit;

	private FormToolkit toolkit;
	private SimpleResultProvider<?> result;

	public TotalImpactResultPage(FormEditor editor,
			SimpleResultProvider<?> result) {
		super(editor, "ImpactResultPage", Messages.LCIAResult);
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText(Messages.LCIAResult);
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		TableViewer impactViewer = createSectionAndViewer(body);
		form.reflow(true);
		impactViewer.setInput(result.getImpactDescriptors());
	}

	private TableViewer createSectionAndViewer(Composite parent) {
		Section section = UI.section(parent, toolkit, Messages.LCIAResult);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);
		String[] columns = { IMPACT_CATEGORY, RESULT, REFERENCE_UNIT };
		TableViewer viewer = Tables.createViewer(composite, columns);
		LCIALabelProvider labelProvider = new LCIALabelProvider();
		viewer.setLabelProvider(labelProvider);
		createColumnSorters(viewer, labelProvider);
		Tables.bindColumnWidths(viewer.getTable(), 0.50, 0.30, 0.2);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
		return viewer;
	}

	private void createColumnSorters(TableViewer viewer, LCIALabelProvider p) {
		Tables.registerSorters(viewer,
				new TableColumnSorter<>(ImpactCategoryDescriptor.class, 0, p),
				new AmountSorter(),
				new TableColumnSorter<>(ImpactCategoryDescriptor.class, 2, p));
	}

	private class LCIALabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			return ImageType.LCIA_CATEGORY_ICON.get();
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
				double val = result.getTotalImpactResult(impactCategory)
						.getValue();
				return Numbers.format(val);
			case 2:
				return impactCategory.getReferenceUnit();
			default:
				return null;
			}
		}
	}

	private class AmountSorter extends
			TableColumnSorter<ImpactCategoryDescriptor> {
		public AmountSorter() {
			super(ImpactCategoryDescriptor.class, 1);
		}

		@Override
		public int compare(ImpactCategoryDescriptor d1,
				ImpactCategoryDescriptor d2) {
			double val1 = result.getTotalImpactResult(d1).getValue();
			double val2 = result.getTotalImpactResult(d2).getValue();
			return Doubles.compare(val1, val2);
		}
	}

}
