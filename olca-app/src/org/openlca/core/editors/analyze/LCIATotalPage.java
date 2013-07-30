package org.openlca.core.editors.analyze;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.UI;
import org.openlca.core.application.Numbers;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.util.Strings;

public class LCIATotalPage extends FormPage {

	private interface COLUMN_LABELS {

		String IMPACT_CATEGORY = "Impact category";
		String REFERENCE_UNIT = "Reference unit";
		String RESULT = "Result";

		String[] VALUES = { IMPACT_CATEGORY, REFERENCE_UNIT, RESULT };

	}

	private static final double[] COLUMN_WIDTHS = { 0.70, 0.20, 0.08 };

	private FormToolkit toolkit;
	private AnalysisResult result;

	public LCIATotalPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, LCIATotalPage.class.getCanonicalName(), "LCIA - Total");
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText("LCIA - Total");
		toolkit.decorateFormHeading(form.getForm());

		Composite body = UI.formBody(form, toolkit);
		TableViewer impactViewer = createSectionAndViewer(body);

		form.reflow(true);

		impactViewer.setInput(result.getImpactCategories());
	}

	private TableViewer createSectionAndViewer(Composite parent) {
		Section section = UI.section(parent, toolkit, "Impact results");
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		TableViewer viewer = new TableViewer(composite);
		viewer.setLabelProvider(new LCIALabelProvider());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);

		for (int i = 0; i < COLUMN_LABELS.VALUES.length; i++) {
			final TableColumn c = new TableColumn(viewer.getTable(), SWT.NULL);
			c.setText(COLUMN_LABELS.VALUES[i]);
		}
		viewer.setColumnProperties(COLUMN_LABELS.VALUES);
		viewer.setSorter(new ImpactViewerSorter());
		UI.gridData(viewer.getTable(), true, true);
		UI.bindColumnWidths(viewer.getTable(), COLUMN_WIDTHS);
		return viewer;
	}

	private class LCIALabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ImpactCategoryDescriptor))
				return null;

			ImpactCategoryDescriptor impactCategory = (ImpactCategoryDescriptor) element;
			String columnLabel = COLUMN_LABELS.VALUES[columnIndex];

			switch (columnLabel) {
			case COLUMN_LABELS.IMPACT_CATEGORY:
				return impactCategory.getName();
			case COLUMN_LABELS.REFERENCE_UNIT:
				return impactCategory.getReferenceUnit();
			case COLUMN_LABELS.RESULT:
				return Numbers.format(result.getResult(result.getSetup()
						.getReferenceProcess(),
						(ImpactCategoryDescriptor) element));
			}

			return null;
		}
	}

	private class ImpactViewerSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof ImpactCategoryDescriptor) || e1 == null) {
				if (e2 != null)
					return -1;
				return 0;
			}
			if (!(e2 instanceof ImpactCategoryDescriptor) || e2 == null)
				return 1;
			ImpactCategoryDescriptor category1 = (ImpactCategoryDescriptor) e1;
			ImpactCategoryDescriptor category2 = (ImpactCategoryDescriptor) e2;
			int compare = Strings.compare(category1.getName(),
					category2.getName());
			if (compare != 0)
				return compare;
			return Strings.compare(category1.getReferenceUnit(),
					category2.getReferenceUnit());
		}

	}

}
