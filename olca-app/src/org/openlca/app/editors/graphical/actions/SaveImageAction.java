package org.openlca.app.editors.graphical.actions;

import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Icon;

import java.io.File;

public class SaveImageAction extends WorkbenchPartAction {

	private String FILE_NAME = "graph.png";

	private final GraphEditor editor;

	public SaveImageAction(GraphEditor part) {
		super(part);
		this.editor = part;
		setId(ActionIds.SAVE_IMAGE);
		setText(M.SaveAsImage);
		setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		return true;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var file = FileChooser.forSavingFile(M.Export, FILE_NAME);
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
			var view = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
			var editPart = (ScalableFreeformRootEditPart) view.getRootEditPart();
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
