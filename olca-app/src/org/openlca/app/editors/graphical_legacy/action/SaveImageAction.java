package org.openlca.app.editors.graphical_legacy.action;

import java.io.File;

import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.rcp.images.Icon;

public class SaveImageAction extends Action implements GraphAction {

	private GraphEditor editor;

	public SaveImageAction() {
		setText(M.SaveAsImage);
		setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
	}

	@Override
	public boolean accepts(GraphEditor editor) {
		this.editor = editor;
		return true;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var file = FileChooser.forSavingFile(M.Export, "graph.png");
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
			var editPart = (ScalableRootEditPart) editor.getGraphicalViewer()
				.getRootEditPart();
			var root = editPart.getLayer(LayerConstants.PRINTABLE_LAYERS);
			var bounds = root.getBounds();
			var img = new Image(null, bounds.width, bounds.height);
			var gc = new GC(img);
			var graphics = new SWTGraphics(gc);
			root.paint(graphics);
			var imgLoader = new ImageLoader();
			imgLoader.data = new ImageData[] { img.getImageData() };
			imgLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
		}
	}
}
