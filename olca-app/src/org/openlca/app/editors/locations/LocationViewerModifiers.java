package org.openlca.app.editors.locations;

import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.Location;
import org.openlca.io.KeyGen;

class LocationViewerModifiers {

	public static void register(ModifySupport<Location> modifySupport,
			LocationsEditor editor) {
		modifySupport
				.bind(LocationViewer.COLUMN_NAME, new NameModifier(editor));
		modifySupport
				.bind(LocationViewer.COLUMN_CODE, new CodeModifier(editor));
		modifySupport.bind(LocationViewer.COLUMN_LATITUDE,
				new LatitudeModifier(editor));
		modifySupport.bind(LocationViewer.COLUMN_LONGITUDE,
				new LongitudeModifier(editor));
	}

	private static class NameModifier extends TextCellModifier<Location> {

		private LocationsEditor editor;

		NameModifier(LocationsEditor editor) {
			this.editor = editor;
		}

		@Override
		protected String getText(Location element) {
			return element.getName();
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			element.setName(text);
			editor.locationChanged(element);
		}

	}

	private static class CodeModifier extends TextCellModifier<Location> {

		private LocationsEditor editor;

		CodeModifier(LocationsEditor editor) {
			this.editor = editor;
		}

		@Override
		protected String getText(Location element) {
			return element.getCode();
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			element.setCode(text);
			element.setRefId(KeyGen.get(text));
			editor.locationChanged(element);
		}

	}

	private static class LatitudeModifier extends TextCellModifier<Location> {

		private LocationsEditor editor;

		LatitudeModifier(LocationsEditor editor) {
			this.editor = editor;
		}

		@Override
		protected String getText(Location element) {
			return Double.toString(element.getLatitude());
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			try {
				element.setLatitude(Double.parseDouble(text));
				editor.locationChanged(element);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	}

	private static class LongitudeModifier extends TextCellModifier<Location> {

		private LocationsEditor editor;

		LongitudeModifier(LocationsEditor editor) {
			this.editor = editor;
		}

		@Override
		protected String getText(Location element) {
			return Double.toString(element.getLongitude());
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			try {
				element.setLongitude(Double.parseDouble(text));
				editor.locationChanged(element);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	}
}
