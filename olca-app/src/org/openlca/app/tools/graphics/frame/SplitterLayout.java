package org.openlca.app.tools.graphics.frame;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

import java.util.Arrays;

public class SplitterLayout extends Layout {

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		var area = composite.getParent().getClientArea();
		if (composite instanceof FigureCanvas) {
			var height = hHint != - 1 ? hHint : area.height;
			return new Point(area.width, height);
		}
		else if (composite instanceof Minimap)
			return new Point(Minimap.DEFAULT_SIZE.width, Minimap.DEFAULT_SIZE.height);
		else if (composite instanceof Header) {
			var width = Header.DEFAULT_SIZE.width == -1
					? wHint
					: Header.DEFAULT_SIZE.width;
			return new Point(width, Header.DEFAULT_SIZE.height);
		} else return new Point(0, 0);
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		var children = composite.getChildren();
		if (children.length == 0)
			return;

		var area = composite.getClientArea();

		var figureCanvas = (FigureCanvas) Arrays.stream(children)
				.filter(control -> control instanceof FigureCanvas)
				.findFirst()
				.orElse(null);
		var map = (Minimap) Arrays.stream(children)
				.filter(control -> control instanceof Minimap)
				.findFirst()
				.orElse(null);
		var header = (Header) Arrays.stream(children)
				.filter(control -> control instanceof Header)
				.findFirst()
				.orElse(null);

		if (figureCanvas != null) {
			if (map != null) {
				map.moveAbove(figureCanvas);
				var size = computeSize(map, SWT.DEFAULT, SWT.DEFAULT, flushCache);
				var xOffset = figureCanvas.getVerticalBar().getSize().x;
				var yOffset = figureCanvas.getHorizontalBar().getSize().y;
				map.setBounds(
						area.width - size.x - xOffset,
						area.height - size.y - yOffset,
						size.x, size.y
				);
			}
			if (header != null) {
				header.moveAbove(figureCanvas);
				var headerSize = computeSize(header, area.width, SWT.DEFAULT, flushCache);
				header.setBounds(0, 0, headerSize.x, headerSize.y);
				var canvasSize = computeSize(figureCanvas, SWT.DEFAULT,
						area.height - headerSize.y, flushCache);
				figureCanvas.setBounds(0, headerSize.y, canvasSize.x, canvasSize.y);
			} else {
				var canvasSize = computeSize(figureCanvas, SWT.DEFAULT, SWT.DEFAULT,
						flushCache);
				figureCanvas.setBounds(0, 0, canvasSize.x, canvasSize.y);
			}
		}
	}

}
