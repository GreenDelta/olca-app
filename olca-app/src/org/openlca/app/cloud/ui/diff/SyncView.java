package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncView extends ViewPart {

	private final static Logger log = LoggerFactory.getLogger(SyncView.class);

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1, 0, 0);
		RepositoryClient client = Database.getRepositoryClient();
		if (client == null)
			return;
		JsonLoader jsonLoader = CloudUtil.getJsonLoader(client);
		SyncDiffViewer viewer = new SyncDiffViewer(body, jsonLoader);
		DiffNode input = getInput();
		if (input != null)
			viewer.setInput(Collections.singleton(input));
	}

	private DiffNode getInput() {
		try {
			RepositoryClient client = Database.getRepositoryClient();
			if (client == null)
				return null;
			DiffIndex index = Database.getDiffIndex();
			List<Dataset> localChanges = new ArrayList<>();
			for (Diff diff : index.getChanged())
				localChanges.add(diff.getDataset());
			List<FetchRequestData> descriptors = client.sync(null, localChanges);
			List<DiffResult> differences = createDifferences(descriptors);
			DiffNode root = new DiffNodeBuilder(client.getConfig().getDatabase(), index).build(differences);
			return root;
		} catch (Exception e) {
			log.error("Error loading remote data", e);
			return null;
		}
	}

	private List<DiffResult> createDifferences(List<FetchRequestData> remotes) {
		DiffIndex index = Database.getDiffIndex();
		List<DiffResult> differences = new ArrayList<>();
		Set<String> added = new HashSet<>();
		for (FetchRequestData identifier : remotes) {
			Diff local = index.get(identifier.refId);
			differences.add(new DiffResult(identifier, local));
			added.add(identifier.refId);
		}
		for (Diff diff : index.getChanged())
			if (!added.contains(diff.getDataset().refId))
				differences.add(new DiffResult(diff));
		return differences;
	}

	@Override
	public void setFocus() {

	}

}
