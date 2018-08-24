package org.openlca.app.cloud.ui.commits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContentProvider implements ITreeContentProvider {

	private RepositoryClient client;

	ContentProvider(RepositoryClient client) {
		this.client = client;
	}

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
		if (!(parentElement instanceof Commit))
			return null;
		Commit commit = (Commit) parentElement;
		try {
			List<FetchRequestData> references = client.getReferences(commit.id);
			Collections.sort(references, new Comparator<FetchRequestData>() {
				@Override
				public int compare(FetchRequestData d1, FetchRequestData d2) {
					String r1 = CloudUtil.getFileReferenceText(d1).toLowerCase();
					String r2 = CloudUtil.getFileReferenceText(d2).toLowerCase();
					return Strings.compare(r1, r2);
				}
			});
			return filterNonCategorized(references).toArray();
		} catch (WebRequestException e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("calling `getChildren` failed", e);
		}
		return null;
	}

	private List<FetchRequestData> filterNonCategorized(
			List<FetchRequestData> references) {
		List<FetchRequestData> filtered = new ArrayList<>();
		for (FetchRequestData reference : references)
			if (reference.type.isCategorized())
				filtered.add(reference);
		return filtered;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Commit)
			return true;
		return false;
	}

}