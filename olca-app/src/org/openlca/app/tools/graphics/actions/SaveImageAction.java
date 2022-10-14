package org.openlca.app.tools.graphics.actions;

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
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.actions.ActionIds;
import org.openlca.app.rcp.images.Icon;

import java.io.File;

public class SaveImageAction extends WorkbenchPartAction {

	private final String fileName;

	private final GraphicalEditor editor;

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
		return true;
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

			var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
			var figureCanvas = viewer.getControl();

			Display.getDefault().syncExec(() -> {
				var bounds = figureCanvas.getBounds();

				var img = new Image(null, bounds.width, bounds.height);
				var gc = new GC(img);

				figureCanvas.print(gc);

				var imgLoader = new ImageLoader();
				imgLoader.data = new ImageData[] { img.getImageData() };
				imgLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);

				gc.dispose();
				img.dispose();
			});
		}
	}

}
