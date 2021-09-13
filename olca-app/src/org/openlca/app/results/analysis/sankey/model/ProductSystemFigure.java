package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.M;
import org.openlca.app.results.analysis.sankey.actions.SankeySelectionAction;
import org.openlca.app.results.analysis.sankey.layout.GraphLayoutManager;
import org.openlca.app.util.Colors;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Question;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;

public class ProductSystemFigure extends Figure {

	private boolean firstTime = true;
	private boolean doPaint = true;
	private final ProductSystemNode node;
	/** Must be disposed when the edit part is deactivated */
	Font infoFont;

	public ProductSystemFigure(ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		this.node = node;
		addMouseListener(new SelectionChange());
	}

	@Override
	public void paint(Graphics graphics) {
		if (firstTime) {
			if (doPaint) {
				doPaint = checkAndAsk();
			}
		}
		if (doPaint) {
			for (var process : node.processNodes) {
				if (!process.figure.isVisible()) {
					process.figure.setVisible(true);
				}
			}
			if (firstTime) {
				firePropertyChange("firstTimeInitialized", null, "not null");
			}
			super.paint(graphics);
			if (firstTime) {
				((GraphLayoutManager) getLayoutManager()).layoutTree();
				firstTime = false;
			}
		}
		paintInfoBox(graphics);
	}

	private boolean checkAndAsk() {
		if (node.processNodes.size() <= 2000)
			return true;
		return Question.ask(M.SankeyDiagram, M.MoreThanXProcesses);
	}

	private void paintInfoBox(Graphics g) {
		g.pushState();
		Font normalFont = g.getFont();
		Font infoFont = getInfoFont(normalFont);
		g.setFont(infoFont);
		Object selection = node.editor.selection;
		double cutoffValue = node.editor.cutoff * 100;
		String cutoffText = M.DontShowSmallerThen + " "
				+ Numbers.format(cutoffValue, 3) + "%";
		if (selection != null) {
			g.drawText(M.ProductSystem + ": "
					+ node.calculationTarget.name, new Point(5, 5));
			String label = selectionLabel(selection);
			g.drawText(label, new Point(5, 30));
			g.drawText(cutoffText, new Point(5, 60));

		} else {
			g.drawText(M.NoAnalysisOptionsSet, new Point(5, 5));
			g.drawText(M.ClickHereToChangeDisplay, new Point(5, 30));
		}
		g.setFont(normalFont);
		drawColorScale(g);
		g.popState();
	}

	private Font getInfoFont(Font normalFont) {
		if (infoFont != null)
			return infoFont;
		infoFont = new Font(Display.getCurrent(),
				new FontData[] { new FontData(normalFont.getFontData()[0].getName(), 16, SWT.NONE) });
		return infoFont;
	}

	private String selectionLabel(Object selection) {
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) selection;
			return M.Flow + ": " + flow.name;
		}
		if (selection instanceof ImpactDescriptor) {
			var impact = (ImpactDescriptor) selection;
			return M.ImpactCategory + ": " + impact.name;
		}
		if (selection instanceof CostResultDescriptor) {
			CostResultDescriptor cost = (CostResultDescriptor) selection;
			String m = M.CostResult;
			return m + ": " + cost.name;
		}
		return M.NoAnalysisOptionsSet;
	}

	private void drawColorScale(Graphics graphics) {
		int x = 25;
		int y = 140;
		for (int i = -100; i < 100; i += 2) {
			graphics.setBackgroundColor(Colors.getForContribution(i / 100d));
			int posX = x + 3 * ((100 + i) / 2);
			graphics.fillRectangle(new Rectangle(posX, y, 4, 20));
		}

		// draw percentage texts
		graphics.drawText(M.Sankey_ScaleDescription, x + 35, y + 22);
		graphics.drawLine(x, y, x + 300, y);
		for (int i = 0; i <= 20; i++) {
			int height;
			if (i == 0 || i == 20) {
				height = 19;
			} else if (i % 2 == 0) {
				height = 12;
			} else {
				height = 6;
			}
			graphics.drawLine(x + 15 * i, y, x + 15 * i, y + height);
			String percentage = "" + (i * 10 - 100);
			if (i % 2 == 0) {
				int left = 5 * (percentage.length() - 1);
				if (i == 10) {
					left += 2;
				} else if (i == 0 || i == 20) {
					left -= 1;
				}
				graphics.drawText(percentage, x + 15 * i - left, y - 16);
			}
		}
	}

	private class SelectionChange extends MouseListener.Stub {

		@Override
		public void mousePressed(MouseEvent arg0) {
			int x = arg0.getLocation().x;
			int y = arg0.getLocation().y;
			if (in(x, 350) && in(y, 120)) {
				new SankeySelectionAction(node.editor).run();
			}
		}

		private boolean in(int val, int max) {
			return val >= 0 && val <= max;
		}
	}

}
