package org.openlca.app.tools.graphics.frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

import java.util.Arrays;

public class MinimapLayout extends Layout {

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		return null;
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		var area = composite.getClientArea();
		var children = composite.getChildren();

		var canvas = (Canvas) Arrays.stream(children)
				.filter(control -> control instanceof Canvas)
				.findFirst()
				.orElse(null);
		var scale = (Minimap.ZoomScale) Arrays.stream(children)
				.filter(control -> control instanceof Minimap.ZoomScale)
				.findFirst()
				.orElse(null);

		if (scale != null) {
			var scaleSize = scale.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			scale.setBounds(0, 0, area.width, scaleSize.y);
			if (canvas != null)
				canvas.setBounds(0, scaleSize.y,
						area.width, area.height - scaleSize.y);
		} else if (canvas != null)
			canvas.setBounds(area);
	}

}
