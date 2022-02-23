package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.SimpleResult;

public class DQInfoSection {

	private final SimpleResult result;
	private final DQResult dqResult;
	private final FormToolkit toolkit;

	public DQInfoSection(Composite parent, FormToolkit toolkit,
			SimpleResult result, DQResult dqResult) {
		this.result = result;
		this.dqResult = dqResult;
		this.toolkit = toolkit;
		create(parent);
	}

	private void create(Composite parent) {
		Section section = UI.section(parent, toolkit, M.DataQuality);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		UI.gridData(client, true, true);
		InfoSection.link(client, M.ProcessDataQualitySchema, dqResult.setup.processSystem);
		InfoSection.link(client, M.FlowDataQualitySchema, dqResult.setup.exchangeSystem);
		InfoSection.text(client, toolkit, M.Aggregation, Labels.of(dqResult.setup.aggregationType));
		InfoSection.text(client, toolkit, M.RoundingMode, dqResult.setup.ceiling ? M.Up : M.HalfUp);
		InfoSection.text(client, toolkit, M.NaValueHandling, Labels.of(dqResult.setup.naHandling));
//		statisticsTree(client, M.ProcessDataQualityStatistics, true);
//		statisticsTree(client, M.FlowDataQualityStatistics, false);
	}

	private void statisticsTree(Composite parent, String label, boolean forProcesses) {
		DQSystem system = forProcesses ? dqResult.setup.processSystem : dqResult.setup.exchangeSystem;
		if (system == null)
			return;
		UI.formLabel(parent, toolkit, label);
		UI.formLabel(parent, toolkit, "");
		String[] headers = { M.Indicator, M.Coverage };
		TreeViewer viewer = Trees.createViewer(parent, headers);
		viewer.setContentProvider(new ContentProvider(forProcesses));
		viewer.setLabelProvider(new LabelProvider(forProcesses));
		((GridData) viewer.getTree().getLayoutData()).horizontalSpan = 2;
		viewer.setInput(system.indicators);
		Trees.bindColumnWidths(viewer.getTree(), 0.6, 0.4);
	}

	private class ContentProvider extends ArrayContentProvider implements ITreeContentProvider {

		private final boolean forProcesses;

		private ContentProvider(boolean forProcesses) {
			this.forProcesses = forProcesses;
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof DQIndicator) && !forProcesses)
				return null;
			List<Object> children = new ArrayList<>();
			for (RootDescriptor p : result.getProcesses()) {
				children.add(new Tuple(p, (DQIndicator) parent));
			}
			return children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof DQIndicator && !forProcesses;
		}

	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		private final boolean forProcesses;

		private LabelProvider(boolean forProcesses) {
			this.forProcesses = forProcesses;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (element instanceof DQIndicator)
				return Images.get(ModelType.DQ_SYSTEM);
			return Images.get(ModelType.PROCESS);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof DQIndicator)
				return getText((DQIndicator) element, columnIndex);
			return getText((Tuple) element, columnIndex);
		}

		private String getText(DQIndicator indicator, int col) {
			switch (col) {
			case 0:
				return indicator.name;
			case 1:
				int withQuality = 0;
				int total = 0;
//				if (forProcesses) {
//					withQuality = dqResult.statistics.getNoOfProcesses(indicator);
//					total = dqResult.statistics.getNoOfProcesses();
//				} else {
//					withQuality = dqResult.statistics.getNoOfExchanges(indicator);
//					total = dqResult.statistics.getNoOfExchanges();
//				}
				return getCoverage(withQuality, total);
			default:
				return null;
			}
		}

		private String getText(Tuple value, int col) {
			switch (col) {
			case 0:
				return value.process.name;
			case 1:
//				int withQuality = dqResult.statistics.getNoOfExchanges(value.process, value.indicator);
//				int total = dqResult.statistics.getNoOfExchanges(value.process);
//				return getCoverage(withQuality, total);
			default:
				return null;
			}
		}

		private String getCoverage(int withQuality, int total) {
			if (total == 0d)
				return "0% (0/0)";
			double coverage = Math.round(10000d * withQuality / total) / 100d;
			return coverage + "% (" + withQuality + "/" + total + ")";
		}
	}

	private record Tuple(RootDescriptor process, DQIndicator indicator) {
	}

}
