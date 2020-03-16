package org.openlca.app.components.mapview;

import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.util.UI;

public class MapDialog {

	private MapDialog() {
	}

	public static void show(String title, Consumer<MapView> fn) {
		_MapDialog dialog = new _MapDialog(title, fn);
		dialog.open();
	}

	private static class _MapDialog extends Dialog {

		private final String title;
		private final Consumer<MapView> fn;

		private _MapDialog(String title, Consumer<MapView> fn) {
			super(UI.shell());
			this.title = title == null ? "Map" : title;
			this.fn = fn;
		}

		@Override
		protected Control createDialogArea(Composite root) {
			Composite area = (Composite) super.createDialogArea(root);
			area.setLayout(new FillLayout());
			getShell().setText(title);
			MapView map = new MapView(area);
			if (fn != null) {
				fn.accept(map);
			}
			return area;
		}

		@Override
		protected Point getInitialSize() {
			Point bounds = UI.shell().getSize();
			int width = (int) (bounds.x * 0.8);
			int height = (int) (bounds.y * 0.8);
			return new Point(width, height);
		}

		@Override
		protected boolean isResizable() {
			return true;
		}
	}
}
