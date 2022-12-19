package org.openlca.app.tools.graphics.actions;

import org.eclipse.draw2d.*;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.edit.RootEditPart;
import org.openlca.app.tools.graphics.zoom.ZoomManager;

import java.io.File;


public class SaveImageAction extends WorkbenchPartAction {

	private final String fileName;
	private final int SCALE = 2;

	private final GraphicalEditor editor;
	private Viewport viewport;
	private ZoomManager zoomManager;
	private ScalableFreeformLayeredPane pane;

	public SaveImageAction(GraphicalEditor part, String fileName) {
		super(part);
		this.editor = part;
		this.fileName = fileName;
		setId(ActionIds.SAVE_IMAGE);
		setText(M.SaveAsImage);
		setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		if (viewer == null)
			return false;
		var root = (RootEditPart) viewer.getRootEditPart();
		if (root == null)
			return false;
		zoomManager = root.getZoomManager();
		pane = (ScalableFreeformLayeredPane) root.getScaledLayers();
		viewport = ((FigureCanvas) viewer.getControl()).getViewport();
		return zoomManager != null && viewport != null && pane != null;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var file = FileChooser.forSavingFile(M.Export, fileName);
		if (file == null)
			return;
		App.run(M.SaveAsImage, new Runner(file));
	}

	private class Runner implements Runnable {

		private final File file;

		public Runner(File file) {
			this.file = file;
		}

		@Override
		public void run() {
			if (file == null)
				return;

			Display.getDefault().syncExec(() -> {
				var location = viewport.getViewLocation();
				var size = viewport.getSize();

				var img = new Image(null, size.width * SCALE, size.height * SCALE);
				var gc = new GC(img);

				var g = new SWTGraphics(gc);
				g.translate(location.negate().getScaled(SCALE));
				g.setAntialias(SWT.ON);
				g.setInterpolation(SWT.HIGH);
				g.setTextAntialias(SWT.ON);

				zoomManager.setZoom(zoomManager.getZoom() * SCALE, false);
				pane.paint(g);
				zoomManager.setZoom(zoomManager.getZoom() / SCALE, false);

				var imgLoader = new ImageLoader();
				imgLoader.data = new ImageData[] { img.getImageData() };
				imgLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);

				gc.dispose();
				img.dispose();
			});
		}

	}

}
