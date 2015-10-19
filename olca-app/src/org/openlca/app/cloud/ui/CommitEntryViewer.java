package org.openlca.app.cloud.ui;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.CommitDescriptor;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.WebRequests.WebRequestException;

class CommitEntryViewer extends AbstractViewer<CommitDescriptor, TreeViewer> {

	private RepositoryClient client;

	public CommitEntryViewer(Composite parent, RepositoryClient client) {
		super(parent);
		this.client = client;
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
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof CommitDescriptor))
				return null;
			CommitDescriptor commit = (CommitDescriptor) parentElement;
			try {
				List<FetchRequestData> references = client.getReferences(commit
						.getId());
				Collections.sort(references, (d1, d2) -> {
					return Strings.compare(getFileReferenceText(d1)
							.toLowerCase(), getFileReferenceText(d2)
							.toLowerCase());
				});
				return references.toArray();
			} catch (WebRequestException e) {
				// TODO handle errors
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof CommitDescriptor)
				return true;
			return false;
		}

	}

	private class LabelProvider extends BaseLabelProvider implements
			ILabelProvider {

		@Override
		public Image getImage(Object element) {
			if (element instanceof CommitDescriptor)
				return ImageManager.getImage(ImageType.COMMIT_ICON);
			if (element instanceof FetchRequestData) {
				FetchRequestData data = (FetchRequestData) element;
				ImageType imageType = null;
				if (data.getType() == ModelType.CATEGORY)
					imageType = Images.getCategoryImageType(data
							.getCategoryType());
				else
					imageType = Images.getImageType(data.getType());
				if (data.isAdded())
					return ImageManager.getImageWithOverlay(imageType,
							ImageType.OVERLAY_ADDED);
				else if (data.isDeleted())
					return ImageManager.getImageWithOverlay(imageType,
							ImageType.OVERLAY_DELETED);
				return ImageManager.getImage(imageType);
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof CommitDescriptor)
				return getCommitText((CommitDescriptor) element);
			if (element instanceof FetchRequestData)
				return getFileReferenceText((FetchRequestData) element);
			return null;
		}

		private String getCommitText(CommitDescriptor descriptor) {
			String text = descriptor.getUser() + ": ";
			text += descriptor.getMessage() + " (";
			text += getTime(descriptor.getTimestamp()) + ")";
			return text;
		}

		private String getTime(long timestamp) {
			Calendar today = Calendar.getInstance();
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp);
			if (cal.after(today))
				return "In future";
			int seconds = getDifference(today, cal, Calendar.SECOND, 60);
			if (seconds < 60)
				return timeText(seconds, "second");
			int minutes = getDifference(today, cal, Calendar.MINUTE, 60);
			if (minutes < 60)
				return timeText(minutes, "minute");
			int hours = getDifference(today, cal, Calendar.HOUR_OF_DAY, 24);
			if (hours < 24)
				return timeText(hours, "hour");
			int days = getDifference(today, cal, Calendar.DAY_OF_MONTH, 365);
			if (days < 7)
				return timeText(days, "day");
			if (days < 31)
				return timeText(days / 7, "week");
			int months = getDifference(today, cal, Calendar.MONTH, 12);
			if (days < 365 && months > 0)
				return timeText(months, "month");
			int years = Calendar.getInstance().get(Calendar.YEAR)
					- cal.get(Calendar.YEAR);
			return timeText(years, "year");
		}

		private int getDifference(Calendar c1, Calendar c2, int type, int max) {
			Calendar tmp = Calendar.getInstance();
			tmp.setTime(c1.getTime());
			int days = -1;
			while (c2.before(tmp)) {
				tmp.add(type, -1);
				days++;
				// more is not of interest here
				if (days == max)
					break;
			}
			return days;
		}

		private String timeText(int value, String timeUnit) {
			return value + " " + timeUnit + (value > 1 ? "s" : "") + " ago";
		}
	}

	private String getFileReferenceText(FetchRequestData reference) {
		String modelType = Labels.modelType(reference.getCategoryType());
		return modelType + "/" + reference.getFullPath();
	}
}
