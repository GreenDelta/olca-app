package org.openlca.app.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openlca.app.navigation.ModelTypeComparison;
import org.openlca.app.util.Labels;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Search implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;

	ModelType typeFilter;
	private String rawTerm;
	private String[] terms;
	private List<BaseDescriptor> result = new ArrayList<>();

	public Search(IDatabase database, String term) {
		this.database = database;
		this.rawTerm = term == null ? "" : term.toLowerCase().trim();
		terms = rawTerm.split(" ");
		for (int i = 0; i < terms.length; i++)
			terms[i] = terms[i].trim();
	}

	public List<BaseDescriptor> getResult() {
		return result;
	}

	@Override
	public void run() {
		result.clear();
		if (rawTerm.isEmpty())
			return;
		log.trace("run search with term {}", rawTerm);
		ModelType[] types = typeFilter == null ? getSearchTypes()
				: new ModelType[] { typeFilter };
		for (ModelType type : types) {
			List<?> descriptors = getDescriptors(type);
			fetchResults(descriptors);
		}
		Collections.sort(result, new ResultComparator());
		log.trace("{} results fetched and ranked", result.size());
	}

	private ModelType[] getSearchTypes() {
		return ModelTypeComparison.getOrderedTypes();
	}

	private List<?> getDescriptors(ModelType type) {
		if (type == ModelType.PARAMETER)
			return new ParameterDao(database).getGlobalDescriptors();
		return Daos.root(database, type).getDescriptors();
	}

	private void fetchResults(List<?> descriptors) {
		for (Object obj : descriptors) {
			if (!(obj instanceof BaseDescriptor))
				continue;
			BaseDescriptor descriptor = (BaseDescriptor) obj;
			if (match(descriptor))
				result.add(descriptor);
		}
	}

	private boolean match(BaseDescriptor d) {
		if (terms == null)
			return false;
		if (terms.length == 1
				&& d.getRefId() != null
				&& d.getRefId().equalsIgnoreCase(terms[0])) {
			return true;
		}
		String label = Labels.getDisplayName(d);
		if (label == null)
			return false;
		String feed = label.toLowerCase();
		for (String term : terms) {
			if (!feed.contains(term))
				return false;
		}
		return true;
	}

	private class ResultComparator implements Comparator<BaseDescriptor> {
		@Override
		public int compare(BaseDescriptor o1, BaseDescriptor o2) {
			String label1 = Labels.getDisplayName(o1).toLowerCase();
			String label2 = Labels.getDisplayName(o2).toLowerCase();
			for (String term : terms) {
				int idx1 = label1.indexOf(term);
				int idx2 = label2.indexOf(term);
				int diff = idx1 - idx2;
				if (diff != 0)
					return diff;
			}
			return label1.compareToIgnoreCase(label2);
		}
	}
}
