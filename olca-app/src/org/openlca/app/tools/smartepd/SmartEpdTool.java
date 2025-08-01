package org.openlca.app.tools.smartepd;

import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.ApiKeyAuth;
import org.openlca.app.tools.smartepd.TreeModel.EpdNode;
import org.openlca.app.tools.smartepd.TreeModel.Node;
import org.openlca.app.tools.smartepd.TreeModel.ProjectNode;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.ModelType;
import org.openlca.io.smartepd.SmartEpdClient;
import org.openlca.util.Res;

public class SmartEpdTool extends SimpleFormEditor {

	private SmartEpdClient client;

	public static void open() {

		var client = ApiKeyAuth.fromCacheOrDialog(
				".smartepd.json", "https://smart-epd.herokuapp.com/api", key -> {
					var c = SmartEpdClient.of(key.endpoint(), key.value());
					var err = c.getProjects();
					return err.hasError()
							? err.wrapError("Failed to connect to API; projects query failed")
							: Res.of(c);
				});
		if (client.isEmpty())
			return;

		var id = AppContext.put(client.get());
		var input = new SimpleEditorInput(id, "SmartEPD");
		Editors.open(input, "SmartEpdTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		var inp = (SimpleEditorInput) input;
		client = AppContext.remove(inp.id, SmartEpdClient.class);
		if (client == null)
			throw new PartInitException("failed to get the SmartEPD client");
		setTitleImage(Icon.SMART_EPD.get());
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final SmartEpdClient client;

		Page(SmartEpdTool editor) {
			super(editor, "SmartEPD", "SmartEPD");
			this.client = editor.client;
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "SmartEPD");
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);
			var tree = Trees.createViewer(body, "Project/EPD");
			tree.getTree().setLinesVisible(false);
			Trees.bindColumnWidths(tree.getTree(), 1.0);
			tree.setLabelProvider(new TreeLabel());
			tree.setContentProvider(new TreeContent());
			TreeMenu.mountOn(client, tree);

			App.runInUI("Fetching data ...", () -> {
				var res = TreeModel.fetch(client);
				if (res.hasError()) {
					ErrorReporter.on(
							"Failed to fetch data from SmartEPD",
							res.error());
				} else {
					tree.setInput(res.value());
					tree.expandAll();
				}
			});
		}

		private static class TreeContent implements ITreeContentProvider {

			@Override
			public Object[] getElements(Object root) {
				return root instanceof TreeModel(List<ProjectNode> projectNodes)
						? projectNodes.toArray()
						: new Object[0];
			}

			@Override
			public Object[] getChildren(Object parent) {
				return parent instanceof ProjectNode node
						? node.epdNodes().toArray()
						: new Object[0];
			}

			@Override
			public Object getParent(Object obj) {
				return obj instanceof EpdNode node
						? node.parent()
						: null;
			}

			@Override
			public boolean hasChildren(Object obj) {
				return obj instanceof ProjectNode node
						&& !node.epdNodes().isEmpty();
			}
		}

		private static class TreeLabel extends BaseLabelProvider
				implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				if (col != 0)
					return null;
				if (obj instanceof ProjectNode) {
					return Icon.SMART_EPD.get();
				}
				if (obj instanceof EpdNode) {
					return Images.get(ModelType.EPD);
				}
				return null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof Node node))
					return null;
				return col == 0 ? node.name() : null;
			}
		}
	}
}
