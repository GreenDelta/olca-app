package org.openlca.app.cloud.ui;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.DiffNodeBuilder.Node;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

import com.greendelta.cloud.model.data.DatasetIdentifier;

public class DiffTreeViewer extends AbstractViewer<Node, TreeViewer> {

	public DiffTreeViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		return viewer;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			Node node = (Node) ((Object[]) inputElement)[0];
			return node.getChildren().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			Node node = (Node) parentElement;
			return node.getChildren().toArray();
		}

		@Override
		public Object getParent(Object element) {
			Node node = (Node) element;
			return node.getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			Node node = (Node) element;
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
			Node node = (Node) element;
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
			Node node = (Node) element;
			if (node.getContent() instanceof DiffResult)
				return getImage((DiffResult) node.getContent());
			if (node.getContent() instanceof ModelType)
				return Images.getIcon(dummyCategory((ModelType) node
						.getContent()));
			return null;
		}

		private Image getImage(DiffResult diff) {
			DatasetIdentifier identifier = diff.getIdentifier();
			ImageType image = null;
			if (identifier.getType() == ModelType.CATEGORY)
				image = Images.getImageType(dummyCategory(identifier
						.getCategoryType()));
			else
				image = Images.getImageType(identifier.getType());
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
