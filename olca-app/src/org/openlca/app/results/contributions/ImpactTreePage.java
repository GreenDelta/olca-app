package org.openlca.app.results.contributions;

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
import org.openlca.app.M;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FlowResult;
import org.openlca.core.results.FullResultProvider;

public class ImpactTreePage extends FormPage {

	private final static String COLUMN_NAME = M.ProcessFlowName;
	private final static String COLUMN_LOCATION = M.Location;
	private final static String COLUMN_CATEGORY = M.FlowCategory;
	private final static String COLUMN_AMOUNT = M.InventoryResult;
	private final static String COLUMN_FACTOR = M.ImpactFactor;
	private final static String COLUMN_IMPACT_RESULT = M.ImpactResult;
	private final static String[] COLUMN_LABELS = { COLUMN_NAME, COLUMN_LOCATION, COLUMN_CATEGORY, COLUMN_AMOUNT,
			COLUMN_FACTOR, COLUMN_IMPACT_RESULT };

	private final FullResultProvider result;
	private final ImpactFactorProvider impactFactors;

	private FormToolkit toolkit;
	private ImpactCategoryViewer categoryViewer;
	private Spinner spinner;
	private TreeViewer viewer;
	private ImpactCategoryDescriptor impactCategory;
	private boolean filterZeroes = true;
	private int cutOff = 10;

	public ImpactTreePage(FormEditor editor, FullResultProvider result, ImpactFactorProvider impactFactors) {
		super(editor, "ImpactTreePage", M.ImpactAnalysis);
		this.result = result;
		this.impactFactors = impactFactors;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, M.ImpactAnalysis);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.ImpactAnalysis);
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);
		createSelectionAndFilter(client);
		createImpactContributionTable(client);
		form.reflow(true);
		categoryViewer.selectFirst();
		spinner.setSelection(2);
	}

	private void createSelectionAndFilter(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		UI.gridLayout(container, 7);
		UI.gridData(container, true, false);
		UI.formLabel(container, M.ImpactCategory);
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
		Button button = UI.formCheckBox(parent, toolkit, M.ExcludeZeroEntries);
		UI.gridData(button, false, false);
		button.setSelection(filterZeroes);
		Controls.onSelect(button, event -> {
			filterZeroes = button.getSelection();
			viewer.refresh();
		});
	}

	private void createCutOffFilter(Composite parent) {
		UI.formLabel(parent, toolkit, M.Cutoff);
		spinner = new Spinner(parent, SWT.BORDER);
		spinner.setValues(cutOff, 0, 100, 0, 1, 10);
		toolkit.adapt(spinner);
		toolkit.createLabel(parent, "%");
		Controls.onSelect(spinner, (e) -> {
			cutOff = spinner.getSelection();
			viewer.refresh();
		});
	}

	private void createImpactContributionTable(Composite parent) {
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		LabelProvider labelProvider = new LabelProvider();
		viewer.setLabelProvider(labelProvider);
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
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
		createColumnSorters(labelProvider);
	}

	private void createColumnSorters(LabelProvider p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 2);
		Viewers.sortByDouble(viewer, p, 4, 5, 6);
	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex > 0)
				return null;
			if (element instanceof ProcessDescriptor)
				return Images.get((ProcessDescriptor) element);
			if (element instanceof FlowWithProcess)
				return Images.get(((FlowWithProcess) element).flow);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ProcessDescriptor)
				return getProcessText((ProcessDescriptor) element, columnIndex);
			if (element instanceof FlowWithProcess)
				return getFlowText((FlowWithProcess) element, columnIndex);
			return null;
		}

		private String getProcessText(ProcessDescriptor descriptor, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return descriptor.getName();
			case 1:
				EntityCache cache = result.cache;
				if (descriptor.getLocation() == null)
					return null;
				Location location = cache.get(Location.class, descriptor.getLocation());
				return location.getName();
			case 5:
				return Numbers.format(getResult(descriptor));
			}
			return null;
		}

		private String getFlowText(FlowWithProcess descriptor, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return descriptor.flow.getName();
			case 2:
				return toString(Labels.getFlowCategory(descriptor.flow, result.cache));
			case 3:
				return Numbers.format(getAmount(descriptor));
			case 4:
				return Numbers.format(getFactor(descriptor));
			case 5:
				return Numbers.format(getResult(descriptor));
			}
			return null;
		}

		private String toString(Pair<String, String> pair) {
			return pair.getLeft() + "/" + pair.getRight();
		}

	}

	private double getAmount(FlowWithProcess d) {
		FlowResult r = result.getSingleFlowResult(d.process, d.flow);
		return r.value;
	}

	private double getFactor(FlowWithProcess descriptor) {
		return impactFactors.get(impactCategory, descriptor);
	}

	private double getResult(FlowWithProcess descriptor) {
		double factor = getFactor(descriptor);
		double amount = getAmount(descriptor);
		return factor * amount;
	}

	private double getResult(ProcessDescriptor d) {
		return result.getSingleImpactResult(d, impactCategory).value;
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
			for (ProcessDescriptor process : result.getProcessDescriptors())
				descriptors.add(process);
			return descriptors.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ProcessDescriptor))
				return null;
			ProcessDescriptor process = (ProcessDescriptor) parentElement;
			List<FlowWithProcess> list = new ArrayList<>();
			for (FlowDescriptor flow : result.getFlowDescriptors())
				list.add(new FlowWithProcess(process, flow));
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

	public class FlowWithProcess {

		public final ProcessDescriptor process;
		public final FlowDescriptor flow;

		private FlowWithProcess(ProcessDescriptor process, FlowDescriptor flow) {
			this.process = process;
			this.flow = flow;
		}
	}

	public class CutOffFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (cutOff == 0)
				return true;
			if (element instanceof ProcessDescriptor) {
				double c = getUpstreamContribution((ProcessDescriptor) element);
				return Math.abs(c * 100) > cutOff;
			}
			return true;
		}

		private double getUpstreamContribution(ProcessDescriptor process) {
			if (process == null)
				return 0;
			double total = result.getTotalImpactResult(impactCategory).value;
			if (total == 0)
				return 0;
			double val = getResult(process);
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

	}

	public class ZeroFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (!filterZeroes)
				return true;
			if (element instanceof FlowWithProcess) {
				FlowWithProcess descriptor = (FlowWithProcess) element;
				double inventory = result.getSingleFlowResult(descriptor.process, descriptor.flow).value;
				if (inventory == 0d)
					return false;
				return getResult(descriptor) != 0d;
			}
			if (element instanceof ProcessDescriptor) {
				ProcessDescriptor descriptor = (ProcessDescriptor) element;
				return getResult(descriptor) != 0d;
			}
			return true;
		}
	}

	public interface ImpactFactorProvider {

		double get(ImpactCategoryDescriptor impactCategory, FlowWithProcess flowWithProcess);

	}

}
