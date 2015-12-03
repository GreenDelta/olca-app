package org.openlca.app.cloud.ui.commits;

import java.util.Calendar;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.core.model.ModelType;

class LabelProvider extends BaseLabelProvider implements
		ILabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof Commit)
			return ImageManager.getImage(ImageType.COMMIT_ICON);
		if (!(element instanceof FetchRequestData))
			return null;
		FetchRequestData data = (FetchRequestData) element;
		ImageType imageType = null;
		if (data.getType() == ModelType.CATEGORY)
			imageType = Images.getCategoryImageType(data.getCategoryType());
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

	@Override
	public String getText(Object element) {
		if (element instanceof Commit)
			return getCommitText((Commit) element);
		if (element instanceof FetchRequestData)
			return CloudUtil
					.getFileReferenceText((FetchRequestData) element);
		return null;
	}

	private String getCommitText(Commit commit) {
		String text = commit.getUser() + ": ";
		text += commit.getMessage() + " (";
		text += getTime(commit.getTimestamp()) + ")";
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