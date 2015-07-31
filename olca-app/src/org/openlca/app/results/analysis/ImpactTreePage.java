package org.openlca.app.results.analysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.FlowResult;
import org.openlca.core.results.FullResultProvider;

public class ImpactTreePage extends FormPage {

	private final static String COLUMN_NAME = "Process/Flow name";
	private final static String COLUMN_LOCATION = "Location";
	private final static String COLUMN_CATEGORY = "Flow category";
	private final static String COLUMN_AMOUNT = "Inventory result";
	private final static String COLUMN_FACTOR = "Impact factor";
	private final static String COLUMN_IMPACT_RESULT = "Impact result";
	private final static String[] COLUMN_LABELS = { COLUMN_NAME, COLUMN_LOCATION, COLUMN_CATEGORY, COLUMN_AMOUNT, COLUMN_FACTOR, COLUMN_IMPACT_RESULT };
	private final FullResultProvider result;
	private FormToolkit toolkit;
	private ImpactCategoryViewer categoryViewer;
	private Button filterZeroButton;
	private Spinner spinner;
	private TreeViewer viewer;
	private ImpactCategoryDescriptor impactCategory;
	private boolean filterZeroes;
	private double cutOff = 0;

	public ImpactTreePage(FormEditor editor, FullResultProvider result) {
		super(editor, "ImpactTreePage", "Impact analysis");
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Impact analysis");
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, "Impact analysis");
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);
		createSelectionAndFilter(client);
		createImpactContributionTable(client);
		form.reflow(true);
		categoryViewer.selectFirst();
	}

	private void createSelectionAndFilter(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		UI.gridLayout(container, 7);
		UI.gridData(container, true, false);
		UI.formLabel(container, "Impact category");
		createCategorySelection(container);
		createCutOffFilter(container);
		createNoImpactFilter(container);
	}

	private void createCategorySelection(Composite parent) {
		categoryViewer = new ImpactCategoryViewer(parent);
		categoryViewer.setInput(result.getImpactDescriptors());
		categoryViewer.addSelectionChangedListener((selection) -> {
			impactCategory = selection;
			viewer.setInput(selection);
		});
	}

	private void createNoImpactFilter(Composite parent) {
		filterZeroButton = UI.formCheckBox(parent, toolkit, "Exclude zero entries");
		Controls.onSelect(filterZeroButton, (event) -> {
			filterZeroes = filterZeroButton.getSelection();
			viewer.refresh();
		});
	}

	private void createCutOffFilter(Composite parent) {
		UI.formLabel(parent, toolkit, Messages.Cutoff);
		spinner = new Spinner(parent, SWT.BORDER);
		spinner.setValues(1, 0, 100, 0, 1, 10);
		toolkit.adapt(spinner);
		toolkit.createLabel(parent, "%");
		Controls.onSelect(spinner, (e) -> {
			cutOff = spinner.getSelection();
			viewer.refresh();
		});
	}

	private void createImpactContributionTable(Composite parent) {
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(new ContentProvider());
		viewer.getTree().setLinesVisible(true);
		viewer.getTree().setHeaderVisible(true);
		for (int i = 0; i < COLUMN_LABELS.length; i++) {
			TreeColumn c = new TreeColumn(viewer.getTree(), SWT.NULL);
			c.setText(COLUMN_LABELS[i]);
			c.pack();
		}
		viewer.setColumnProperties(COLUMN_LABELS);
		viewer.addFilter(new CutOffFilter());
		viewer.addFilter(new ZeroFilter());
		toolkit.adapt(viewer.getTree(), false, false);
		toolkit.paintBordersFor(viewer.getTree());
		UI.gridData(viewer.getTree(), true, true);
	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex > 0)
				return null;
			if (element instanceof ProcessDescriptor)
				return Images.getIcon(ModelType.PROCESS);
			if (element instanceof FlowWithProcessDescriptor)
				return Images.getIcon(ModelType.FLOW);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String column = COLUMN_LABELS[columnIndex];
			if (element instanceof ProcessDescriptor)
				return getProcessText((ProcessDescriptor) element, column);
			if (element instanceof FlowWithProcessDescriptor)
				return getFlowText((FlowWithProcessDescriptor) element, column);
			return null;
		}

		private String getProcessText(ProcessDescriptor descriptor, String column) {
			switch (column) {
			case COLUMN_NAME:
				return descriptor.getName();
			case COLUMN_LOCATION:
				EntityCache cache = result.getCache();
				if (descriptor.getLocation() == null)
					return null;
				Location location = cache.get(Location.class, descriptor.getLocation());
				return location.getName();
			case COLUMN_IMPACT_RESULT:
				return Double.toString(getResult(descriptor));
			}
			return null;
		}

		private String getFlowText(FlowWithProcessDescriptor descriptor, String column) {
			switch (column) {
			case COLUMN_NAME:
				return descriptor.flow.getName();
			case COLUMN_CATEGORY:
				return toString(Labels.getFlowCategory(descriptor.flow, result.getCache()));
			case COLUMN_AMOUNT:
				return Double.toString(getAmount(descriptor));
			case COLUMN_FACTOR:
				return Double.toString(getFactor(descriptor));
			case COLUMN_IMPACT_RESULT:
				return Double.toString(getResult(descriptor));
			}
			return null;
		}

		private String toString(Pair<String, String> pair) {
			return pair.getLeft() + "/" + pair.getRight();
		}

	}

	private double getAmount(FlowWithProcessDescriptor descriptor) {
		FlowResult flowResult = result.getSingleFlowResult(descriptor.process, descriptor.flow);
		return flowResult.getValue();
	}

	private double getFactor(FlowWithProcessDescriptor descriptor) {
		int row = result.getResult().getImpactIndex().getIndex(impactCategory.getId());
		int col = result.getResult().getFlowIndex().getIndex(descriptor.flow.getId());
		return result.getResult().getImpactFactorMatrix().getEntry(row, col);
	}

	private double getResult(FlowWithProcessDescriptor descriptor) {
		double factor = getFactor(descriptor);
		double amount = getAmount(descriptor);
		return factor * amount;
	}

	private double getResult(ProcessDescriptor descriptor) {
		return result.getSingleImpactResult(descriptor, impactCategory).getValue();
	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (!(inputElement instanceof ImpactCategoryDescriptor))
				return null;
			List<ProcessDescriptor> descriptors = new ArrayList<>();
			for (ProcessDescriptor process : result.getProcessDescriptors()) {
				double value = result.getSingleImpactResult(process, impactCategory).getValue();
				if (value > 0)
					descriptors.add(process);
			}
			return descriptors.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ProcessDescriptor))
				return null;
			ProcessDescriptor process = (ProcessDescriptor) parentElement;
			List<FlowWithProcessDescriptor> list = new ArrayList<>();
			for (FlowDescriptor flow : result.getFlowDescriptors())
				list.add(new FlowWithProcessDescriptor(process, flow));
			return list.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof ProcessDescriptor))
				return false;
			return true;
		}

	}

	private class FlowWithProcessDescriptor {

		private ProcessDescriptor process;
		private FlowDescriptor flow;

		private FlowWithProcessDescriptor(ProcessDescriptor process, FlowDescriptor flow) {
			this.process = process;
			this.flow = flow;
		}
	}

	public class CutOffFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (cutOff == 0d)
				return true;
			if (element instanceof FlowWithProcessDescriptor) {
				FlowWithProcessDescriptor descriptor = (FlowWithProcessDescriptor) element;
				return getResult(descriptor) > 0d;
			}
			if (element instanceof ProcessDescriptor) {
				ProcessDescriptor descriptor = (ProcessDescriptor) element;
				return getResult(descriptor) > 0d;
			}
			return true;
		}
	}

	public class ZeroFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!filterZeroes)
				return true;
			if (element instanceof FlowWithProcessDescriptor) {
				FlowWithProcessDescriptor descriptor = (FlowWithProcessDescriptor) element;
				double inventory = result.getSingleFlowResult(descriptor.process, descriptor.flow).getValue();
				if (inventory == 0d)
					return false;
				return getResult(descriptor) > 0d;
			}
			if (element instanceof ProcessDescriptor) {
				ProcessDescriptor descriptor = (ProcessDescriptor) element;
				return getResult(descriptor) > 0d;
			}
			return true;
		}
	}

}
