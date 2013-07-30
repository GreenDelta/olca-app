/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze.sankey;

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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.Colors;
import org.openlca.core.application.FaviColor;
import org.openlca.core.application.Messages;
import org.openlca.core.application.Numbers;
import org.openlca.core.editors.analyze.AnalyzeActionContributor;
import org.openlca.core.editors.analyze.PropertySelectionAction;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * Figure of the product system.
 */
public class ProductSystemFigure extends Figure {

	private boolean firstTime = true;
	private ProductSystemNode productSystemNode;
	private Font infoFont;

	public ProductSystemFigure(ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		productSystemNode = node;
		addMouseListener(new SelectionChange());
	}

	Font getInfoFont() {
		return infoFont;
	}

	@Override
	public void paint(Graphics graphics) {
		if (firstTime) {
			firePropertyChange("firstTimeInitialized", null, "not null");
		}
		super.paint(graphics);
		if (firstTime) {
			((GraphLayoutManager) getLayoutManager()).layoutTree();
			firstTime = false;
		}
		paintInfoBox(graphics);
	}

	private void paintInfoBox(Graphics graphics) {
		graphics.pushState();
		Font normalFont = graphics.getFont();
		Font infoFont = getInfoFont(normalFont);
		graphics.setFont(infoFont);

		Object selection = productSystemNode.getSelection();
		double cutoffValue = productSystemNode.getCutoff() * 100;
		String cutoffText = Messages.Common_CutOff + ": "
				+ Numbers.format(cutoffValue, 3) + "%";
		if (selection != null) {
			String label = selectionLabel(selection);
			graphics.drawText(Messages.Common_ProductSystem + ": "
					+ productSystemNode.getProductSystem().getName(),
					new Point(5, 5));
			graphics.drawText(label, new Point(5, 30));
			graphics.drawText(cutoffText, new Point(5, 60));

		} else {
			graphics.drawText(Messages.Sankey_NoOptions, new Point(5, 5));
			graphics.drawText(Messages.Sankey_ClickHere, new Point(5, 30));
		}

		graphics.setFont(normalFont);

		drawColorScale(graphics);
		graphics.popState();
	}

	private Font getInfoFont(Font normalFont) {
		if (infoFont != null)
			return infoFont;
		infoFont = new Font(Display.getCurrent(),
				new FontData[] { new FontData(
						normalFont.getFontData()[0].getName(), 16, SWT.NONE) });
		return infoFont;
	}

	private String selectionLabel(Object selection) {
		if (selection instanceof Flow) {
			Flow flow = (Flow) selection;
			return Messages.Common_Flow + ": " + flow.getName();
		}
		if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor category = (ImpactCategoryDescriptor) selection;
			return Messages.Common_ImpactCategory + ": " + category.getName();
		}
		return Messages.Sankey_NoOptions;
	}

	private void drawColorScale(Graphics graphics) {
		int x = 25;
		int y = 140;
		for (int i = 0; i < 10; i++) {
			RGB rgb = FaviColor.getContributionColor(i);
			graphics.setBackgroundColor(Colors.getColor(rgb));
			int posX = x + 30 * i;
			graphics.fillRectangle(new Rectangle(posX, y, 30, 20));
		}

		// draw percentage texts
		graphics.drawText(Messages.Sankey_ScaleDescription, x + 35, y + 22);
		graphics.drawLine(x, y, x + 300, y);
		for (int i = 0; i <= 20; i++) {
			int height = 0;
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

	private class SelectionChange implements MouseListener {

		@Override
		public void mouseReleased(MouseEvent evt) {
		}

		@Override
		public void mouseDoubleClicked(MouseEvent evt) {
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			int x = arg0.getLocation().x;
			int y = arg0.getLocation().y;
			if (in(x, 350) && in(y, 120)) {
				PropertySelectionAction psa = AnalyzeActionContributor
						.getInstance().getPropertySelectionAction();
				psa.setSankeyDiagram(productSystemNode.getEditor());
				psa.run();
			}
		}

		private boolean in(int val, int max) {
			return val >= 0 && val <= max;
		}
	}

}
