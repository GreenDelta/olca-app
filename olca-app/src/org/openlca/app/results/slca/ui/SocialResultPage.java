package org.openlca.app.results.slca.ui;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.slca.SocialResult;
import org.openlca.app.results.slca.ui.TreeModel.IndicatorNode;
import org.openlca.app.results.slca.ui.TreeModel.Node;
import org.openlca.app.results.slca.ui.TreeModel.TechFlowNode;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.RiskLevel;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SocialResultPage extends FormPage {

	private final ResultEditor editor;
	private final SocialResult result;

	public SocialResultPage(ResultEditor editor, SocialResult result) {
		super(editor, "SocialResultPage", "Social assessment");
		this.editor = editor;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm,
				Labels.name(editor.setup().target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var section = UI.section(body, tk, "Indicator results");
		var comp = UI.sectionClient(section, tk, 1);
		UI.gridData(section, true, true);

		// create headers and tree
		var levels = RiskLevel.values();
		var headers = new String[3 + levels.length];
		headers[0] = "";
		headers[1] = "Activity value";
		headers[2] = "Raw value";
		for (var rl : levels) {
			int col = TreeGrid.columnOf(rl);
			if (col < 0 || col >= headers.length)
				continue;
			headers[col] = TreeGrid.headerOf(rl);
		}
		var tree = Trees.createViewer(comp, headers);

		// bind column widths
		double[] widths = new double[headers.length];
		widths[0] = 0.2;
		widths[1] = 0.15;
		widths[2] = 0.15;
		for (int i = 3; i < widths.length; i++) {
			widths[i] = 0.5 / levels.length;
		}
		Trees.bindColumnWidths(tree.getTree(), widths);

		// set column tool tips
		for (var level : levels) {
			int col = TreeGrid.columnOf(level);
			var t = tree.getTree();
			if (col < 0 || col >= t.getColumnCount())
				continue;
			t.getColumn(col).setToolTipText(Labels.of(level));
		}

		// set providers
		tree.setLabelProvider(new TreeLabel());
		tree.setComparator(new TreeSorter());
		var model = new TreeModel(result);
		tree.setContentProvider(model);

		// bind actions
		var onOpen = Actions.onOpen(() -> {
			var obj = Viewers.getFirstSelected(tree);
			if (obj == null)
				return;
			if (obj instanceof TechFlowNode t) {
				App.open(t.techFlow().provider());
				return;
			}
			if (obj instanceof IndicatorNode i) {
				App.open(i.descriptor());
			}
		});
		Actions.bind(tree, onOpen);

		tree.setInput(model);
	}

	private static class TreeLabel extends BaseLabelProvider
			implements ITableLabelProvider, ITableColorProvider {

		private final ContributionImage img = new ContributionImage();
		private final DecimalFormat percentage = new DecimalFormat(
				"#0%", new DecimalFormatSymbols(Locale.US));

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Node n))
				return null;
			if (col == 0)
				return n.icon();
			if (col == 1) {
				return n instanceof TechFlowNode t
						? img.get(t.activityShare())
						: img.get(1, Colors.background());
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Node n))
				return null;
			return switch (col) {
				case 0 -> n.name();
				case 1 -> n.activityValue();
				case 2 -> n.rawValue();
				default -> {
					var level = TreeGrid.levelOf(col);
					if (level == null)
						yield null;
					var value = n.riskValue().getShare(level);
					yield percentage.format(value);
				}
			};
		}

		@Override
		public Color getForeground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getBackground(Object obj, int col) {
			if (col < 2 || !(obj instanceof Node n))
				return null;
			var level = TreeGrid.levelOf(col);
			if (level == null)
				return null;

			var share = n.riskValue().getShare(level);
			return share >= 0.005
					? TreeGrid.colorOf(level)
					: null;
		}
	}
}
