package org.openlca.app.cloud;

import java.util.Calendar;

import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.util.Labels;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

public class CloudUtil {

	public static Dataset toDataset(INavigationElement<?> element) {
		CategorizedDescriptor descriptor = null;
		if (element instanceof CategoryElement) {
			descriptor = Descriptors.toDescriptor(((CategoryElement) element)
					.getContent());
		} else if (element instanceof ModelElement)
			descriptor = ((ModelElement) element).getContent();
		if (descriptor == null)
			return null;
		Category category = null;
		if (element.getParent() instanceof CategoryElement)
			category = ((CategoryElement) element.getParent()).getContent();
		return toDataset(descriptor, category);
	}

	public static Dataset toDataset(CategorizedEntity entity) {
		CategorizedDescriptor descriptor = Descriptors.toDescriptor(entity);
		Category category = entity.getCategory();
		return toDataset(descriptor, category);
	}

	public static Dataset toDataset(CategorizedDescriptor descriptor, Category category) {
		Dataset dataset = new Dataset();
		dataset.name = descriptor.getName();
		dataset.refId = descriptor.getRefId();
		dataset.type = descriptor.getModelType();
		dataset.version = Version.asString(descriptor.getVersion());
		dataset.lastChange = descriptor.getLastChange();
		ModelType categoryType = null;
		if (category != null) {
			dataset.categoryRefId = category.getRefId();
			categoryType = category.getModelType();
		} else {
			if (descriptor.getModelType() == ModelType.CATEGORY)
				categoryType = ((CategoryDescriptor) descriptor).getCategoryType();
			else
				categoryType = descriptor.getModelType();
		}
		dataset.categoryType = categoryType;
		dataset.fullPath = getFullPath(descriptor, category);
		return dataset;
	}

	public static String getFullPath(Category category) {
		return getFullPath(Descriptors.toDescriptor(category), category.getCategory());
	}

	public static String getFullPath(CategorizedDescriptor entity, Category category) {
		String path = entity.getName();
		while (category != null) {
			path = category.getName() + "/" + path;
			category = category.getCategory();
		}
		return path;
	}

	public static String getFileReferenceText(FetchRequestData reference) {
		String modelType = Labels.modelType(reference.categoryType);
		return modelType + "/" + reference.fullPath;
	}

	public static JsonLoader getJsonLoader(RepositoryClient client) {
		return new JsonLoader(client);
	}

	public static String formatCommitDate(long value) {
		Calendar today = Calendar.getInstance();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(value);
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
		int years = Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR);
		return timeText(years, "year");
	}

	private static int getDifference(Calendar c1, Calendar c2, int type, int max) {
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

	private static String timeText(int value, String timeUnit) {
		return value + " " + timeUnit + (value > 1 ? "s" : "") + " ago";
	}
}
