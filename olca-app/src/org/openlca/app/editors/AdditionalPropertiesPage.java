package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.RootEntity;
import org.openlca.util.Strings;

public class AdditionalPropertiesPage<T extends RootEntity> extends ModelPage<T> {

	public AdditionalPropertiesPage(ModelEditor<T> editor) {
		super(editor, "AdditionalPropertiesPage", M.AdditionalProperties);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var tree = Trees.createViewer(body, "Key", "Value");
		tree.setLabelProvider(new JsonLabel());
		tree.setAutoExpandLevel(2);
		tree.setContentProvider(new JsonContent());
		tree.setInput(getModel().readOtherProperties());
		UI.gridData(tree.getTree(), true, true);
		Trees.bindColumnWidths(tree.getTree(), .25, .75);
		if (isEditable()) {
			var onEdit = Actions.onEdit(
					() -> new JsonDialog(tree).open());
			Actions.bind(tree, onEdit);
		}
	}

	private class JsonDialog extends Dialog {

		private final TreeViewer tree;
		private StyledText text;

		JsonDialog(TreeViewer tree) {
			super(UI.shell());
			this.tree = tree;
		}

		@Override
		protected Control createDialogArea(Composite root) {
			getShell().setText("Edit additional properties");
			var comp = (Composite) super.createDialogArea(root);
			UI.gridLayout(comp, 1);
			new Label(comp, SWT.NONE).setText(
					"The content must be a valid JSON object, see json.org");
			text = new StyledText(comp,
					SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			text.setAlwaysShowScrollBars(false);
			UI.gridData(text, true, true);
			text.setText(getJsonText());
			return comp;
		}

		private String getJsonText() {
			var model = getModel();
			var props = model.readOtherProperties();
			if (props == null)
				return "{}";
			return new GsonBuilder()
					.setPrettyPrinting()
					.create()
					.toJson(props);
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 400);
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected void okPressed() {
			var json = text.getText();
			var model = getModel();
			try {
				var obj = Strings.notEmpty(json)
						? new Gson().fromJson(json, JsonObject.class)
						: new JsonObject();
				model.writeOtherProperties(obj);
				getEditor().setDirty(true);
				tree.setInput(obj);
				super.okPressed();
			} catch (Exception e) {
				MsgBox.error("Failed to parse JSON",
						"Please check the format of the given JSON string.");
			}
		}
	}

	private record Entry(String key, JsonElement value) {

		static Entry of(String key, JsonElement value) {
			return new Entry(key, value);
		}

		static List<Entry> membersOf(JsonObject obj) {
			if (obj == null)
				return List.of();
			var members = new ArrayList<Entry>();
			for (var e : obj.entrySet()) {
				if (e.getKey() != null && e.getValue() != null) {
					members.add(Entry.of(e.getKey(), e.getValue()));
				}
			}
			return members;
		}

		static List<Entry> membersOf(JsonArray array) {
			if (array == null)
				return List.of();
			var members = new ArrayList<Entry>();
			for (int i = 0; i < array.size(); i++) {
				var elem = array.get(i);
				members.add(Entry.of(Integer.toString(i), elem));
			}
			return members;
		}
	}

	private static class JsonContent implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parent) {
			return parent instanceof Entry e
					? getElements(e.value())
					: null;
		}

		@Override
		public Object[] getElements(Object input) {
			if (input instanceof JsonObject obj)
				return Entry.membersOf(obj).toArray();
			if (input instanceof JsonArray array)
				return Entry.membersOf(array).toArray();
			return null;
		}

		@Override
		public Object getParent(Object elem) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof Entry e))
				return false;
			if (e.value() instanceof JsonArray array)
				return !array.isEmpty();
			if (e.value() instanceof JsonObject obj)
				return !obj.entrySet().isEmpty();
			return false;
		}
	}

	private static class JsonLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Entry e))
				return null;
			if (col == 0)
				return e.key();
			if (col != 1)
				return null;
			if (!(e.value() instanceof JsonPrimitive p))
				return null;
			if (p.isBoolean())
				return Boolean.toString(p.getAsBoolean());
			if (p.isNumber())
				return Double.toString(p.getAsDouble());
			if (p.isString())
				return p.getAsString();
			if (p.isJsonNull())
				return "null";
			return p.toString();
		}

	}

}
