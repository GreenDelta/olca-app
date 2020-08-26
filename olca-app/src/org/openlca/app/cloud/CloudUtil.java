package org.openlca.app.cloud;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.util.Labels;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.Datasets;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

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
		return Datasets.toDataset(descriptor, category);
	}

	public static String getFileReferenceText(FetchRequestData reference) {
		String modelType = Labels.modelType(reference.categoryType);
		return modelType + "/" + toFullPath(reference);
	}

	public static String toFullPath(Dataset dataset) {
		if (dataset.categories == null || dataset.categories.size() == 0)
			return dataset.name;
		return Strings.join(dataset.categories, '/') + "/" + dataset.name;
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

	public static FetchRequestData toFetchRequestData(Dataset dataset) {
		FetchRequestData data = new FetchRequestData();
		data.type = dataset.type;
		data.refId = dataset.refId;
		data.name = dataset.name;
		data.version = dataset.version;
		data.lastChange = dataset.lastChange;
		data.categoryType = dataset.categoryType;
		data.categoryRefId = dataset.categoryRefId;
		if (dataset.categories != null) {
			data.categories = new ArrayList<>(dataset.categories);
		}
		data.tags = dataset.tags;
		return data;
	}

	public static boolean commitIsAhead(Commit commit, List<Commit> commits) {
		RepositoryClient client = Database.getRepositoryClient();
		if (client == null)
			return false;
		String lastCommitId = client.getConfig().getLastCommitId();
		if (commits == null) {
			try {
				commits = client.fetchCommitHistory();
			} catch (WebRequestException e) {
				LoggerFactory.getLogger(CloudUtil.class).error("Error during fetch commit history", e);
				return false;
			}
		}
		if (commits.size() == 0)
			return false;
		if (commit == null) {
			commit = commits.get(commits.size() - 1);
		}
		if (lastCommitId == null)
			return true;
		for (Commit c : commits) {
			if (c.id.equals(lastCommitId))
				return false;
			if (c.id.equals(commit.id))
				return true;
		}
		return true;
	}

	public static Map<String, Category> buildCategoryMap(RepositoryClient client) {
		Map<String, Category> categories = new HashMap<>();
		putCategories(new CategoryDao(client.getConfig().database).getRootCategories(), categories);
		return categories;
	}

	private static void putCategories(List<Category> categories, Map<String, Category> map) {
		for (Category c : categories) {
			map.put(c.refId, c);
			putCategories(c.childCategories, map);
		}
	}

}
