package org.openlca.app.cloud.ui;

import java.util.function.Function;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.DiffNodeBuilder.DiffNode;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

import com.google.gson.JsonObject;
import com.greendelta.cloud.model.data.DatasetDescriptor;
	
public class DiffTreeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	private Function<DiffResult, JsonObject> getLocalJson;
	private Function<DiffResult, JsonObject> getRemoteJson;

	public DiffTreeViewer(Composite parent,
			Function<DiffResult, JsonObject> getJson) {
		this(parent, getJson, getJson);
	}

	public DiffTreeViewer(Composite parent,
			Function<DiffResult, JsonObject> getLocalJson,
			Function<DiffResult, JsonObject> getRemoteJson) {
		super(parent);
		this.getLocalJson = getLocalJson;
		this.getRemoteJson = getRemoteJson;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.addDoubleClickListener(this::onDoubleClick);
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		return viewer;
	}

	private void onDoubleClick(DoubleClickEvent event) {
		if (event.getSelection().isEmpty())
			return;
		if (!(event.getSelection() instanceof IStructuredSelection))
			return;
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.size() > 1)
			return;
		DiffNode selected = (DiffNode) selection.getFirstElement();
		if (!(selected.getContent() instanceof DiffResult))
			return;
		DiffResult result = (DiffResult) selected.getContent();
		JsonObject local = getLocalJson.apply(result);
		JsonObject remote = getRemoteJson.apply(result);
		new DiffEditorDialog(local, remote).open();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			DiffNode node = (DiffNode) ((Object[]) inputElement)[0];
			return node.getChildren().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			DiffNode node = (DiffNode) parentElement;
			return node.getChildren().toArray();
		}

		@Override
		public Object getParent(Object element) {
			DiffNode node = (DiffNode) element;
			return node.getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			DiffNode node = (DiffNode) element;
			return !node.getChildren().isEmpty();
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public String getText(Object element) {
			if (element == null)
				return null;
			DiffNode node = (DiffNode) element;
			if (node.getContent() instanceof DiffResult)
				return ((DiffResult) node.getContent()).getDisplayName();
			if (node.getContent() instanceof ModelType)
				return Labels.modelType((ModelType) node.getContent());
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element == null)
				return null;
			DiffNode node = (DiffNode) element;
			if (node.getContent() instanceof DiffResult)
				return getImage((DiffResult) node.getContent());
			if (node.getContent() instanceof ModelType)
				return Images.getIcon(dummyCategory((ModelType) node
						.getContent()));
			return null;
		}

		private Image getImage(DiffResult diff) {
			DatasetDescriptor descriptor = diff.getDescriptor();
			ImageType image = null;
			if (descriptor.getType() == ModelType.CATEGORY)
				image = Images.getImageType(dummyCategory(descriptor
						.getCategoryType()));
			else
				image = Images.getImageType(descriptor.getType());
			ImageType overlay = getOverlay(diff.getType());
			if (overlay == null)
				return ImageManager.getImage(image);
			return ImageManager.getImageWithOverlay(image, overlay);
		}

		private ImageType getOverlay(DiffResponse response) {
			if (response == null)
				return null;
			switch (response) {
			case ADD_TO_LOCAL:
				return ImageType.OVERLAY_ADD_TO_LOCAL;
			case ADD_TO_REMOTE:
				return ImageType.OVERLAY_ADD_TO_REMOTE;
			case MODIFY_IN_LOCAL:
				return ImageType.OVERLAY_MODIFY_IN_LOCAL;
			case MODIFY_IN_REMOTE:
				return ImageType.OVERLAY_MODIFY_IN_REMOTE;
			case CONFLICT:
				return ImageType.OVERLAY_CONFLICT;
			case DELETE_FROM_LOCAL:
				return ImageType.OVERLAY_DELETE_FROM_LOCAL;
			case DELETE_FROM_REMOTE:
				return ImageType.OVERLAY_DELETE_FROM_REMOTE;
			default:
				return null;
			}
		}

		private Category dummyCategory(ModelType type) {
			Category dummy = new Category();
			dummy.setModelType(type);
			return dummy;
		}
	}

}
