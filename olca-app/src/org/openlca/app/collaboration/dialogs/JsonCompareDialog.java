package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.viewers.json.JsonCompareViewer;
import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.IJsonNodeLabelProvider;
import org.openlca.app.collaboration.viewers.json.olca.ModelDependencyResolver;
import org.openlca.app.collaboration.viewers.json.olca.ModelLabelProvider;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;

public class JsonCompareDialog extends FormDialog {

	public final static int KEEP = 2;
	public final static int OVERWRITE = 3;
	private final JsonNode root;
	private final boolean canMerge;
	private JsonCompareViewer viewer;
	private IJsonNodeLabelProvider labelProvider;
	private IDependencyResolver dependencyResolver;
	private String title;
	private Image logo;

	public static JsonCompareDialog forComparison(JsonNode node) {
		return create(node, false);
	}

	public static JsonCompareDialog forMerging(JsonNode node) {
		return create(node, true);
	}

	private static JsonCompareDialog create(JsonNode node, boolean canMerge) {
		if (node == null)
			return null;
		var dialog = new JsonCompareDialog(node, canMerge);
		dialog.setTitle(Json.getName(node.element()));
		dialog.setLogo(Images.get(Json.getModelType(node.element())));
		dialog.setDependencyResolver(ModelDependencyResolver.INSTANCE);
		dialog.setLabelProvider(new ModelLabelProvider());
		return dialog;
	}

	private JsonCompareDialog(JsonNode root, boolean canMerge) {
		super(UI.shell());
		this.root = root;
		this.canMerge = canMerge;
		setBlockOnOpen(true);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var title = canMerge ? M.Merge : M.Compare;
		if (this.title != null) {
			title += ": " + this.title;
		}
		var form = UI.formHeader(mform, title);
		if (logo != null) {
			form.setImage(logo);
		}
		var toolkit = mform.getToolkit();
		var body = UI.formBody(form, toolkit);
		viewer = canMerge
				? JsonCompareViewer.forMerging(body, toolkit, root)
				: JsonCompareViewer.forComparison(body, toolkit, root);
		viewer.initialize(labelProvider, dependencyResolver);
		UI.gridData(viewer, true, true);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var hasLeft = root.left != null;
		var hasRight = root.right != null;
		if (!canMerge) {
			createButton(parent, IDialogConstants.OK_ID, M.Close, true);
		} else if (hasLeft && hasRight) {
			createButton(parent, IDialogConstants.OK_ID, M.MarkAsMerged, true);
		} else if (hasRight) {
			createButton(parent, KEEP, M.KeepDatasetDeleted, true);
			createButton(parent, OVERWRITE, M.ImportRemoteDataset, false);
		} else {
			createButton(parent, KEEP, M.KeepLocalDataset, false);
			createButton(parent, OVERWRITE, M.DeleteLocalDataset, true);
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1000, 600);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLogo(Image logo) {
		this.logo = logo;
	}

	public void setDependencyResolver(IDependencyResolver dependencyResolver) {
		this.dependencyResolver = dependencyResolver;
	}

	public void setLabelProvider(IJsonNodeLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == KEEP || buttonId == OVERWRITE) {
			setReturnCode(buttonId);
			close();
		}
	}

}
