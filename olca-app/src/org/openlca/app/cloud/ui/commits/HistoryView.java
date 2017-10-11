package org.openlca.app.cloud.ui.commits;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.ModelLabelProvider;
import org.openlca.app.cloud.ui.compare.ModelNodeBuilder;
import org.openlca.app.cloud.ui.compare.ModelUtil;
import org.openlca.app.cloud.ui.compare.json.DiffEditor;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

public class HistoryView extends ViewPart {

	public final static String ID = "views.cloud.history";
	private final static Logger log = LoggerFactory.getLogger(HistoryView.class);
	private static HistoryView instance;
	private HistoryViewer historyViewer;
	private ReferencesViewer referencesViewer;
	private DiffEditor diffViewer;
	private Commit current;

	public HistoryView() {
		instance = this;
	}

	@Override
	public void createPartControl(Composite parent) {
		SashForm body = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
		UI.gridData(body, true, true);
		UI.gridLayout(body, 1);
		createHistoryViewer(body);
		SashForm secondRow = new SashForm(body, SWT.HORIZONTAL | SWT.SMOOTH);
		createJsonViewer(secondRow);
		createReferencesViewer(secondRow);
		refresh();
	}

	private void createHistoryViewer(Composite parent) {
		historyViewer = new HistoryViewer(parent);
		UI.gridData(historyViewer.getViewer().getTable(), true, true);
		Tables.bindColumnWidths(historyViewer.getViewer(), 0.1, 0.7, 0.1, 0.1);
		historyViewer.addSelectionChangedListener((commit) -> {
			if (commit == null)
				return;
			if (commit.equals(current))
				return;
			current = commit;
			instance.diffViewer.setInput(null);
			loadReferences(commit);
		});
	}

	private void createJsonViewer(Composite parent) {
		diffViewer = DiffEditor.forViewing(parent);
		diffViewer.setLabels(M.SelectedCommit, M.PreviousCommit);
		diffViewer.initialize(null, new ModelLabelProvider(), ModelUtil.getDependencyResolver(), Direction.LEFT_TO_RIGHT);
	}

	private void createReferencesViewer(Composite parent) {
		referencesViewer = new ReferencesViewer(parent);
		UI.gridData(referencesViewer.getViewer().getTable(), true, true);
		referencesViewer.addSelectionChangedListener((ref) -> {
			if (ref == null)
				return;
			if (!Database.isConnected())
				return;
			if (ref.equals(referencesViewer.lastSelection))
				return;
			referencesViewer.lastSelection = ref;
			RepositoryClient client = Database.getRepositoryClient();
			JsonLoader loader = CloudUtil.getJsonLoader(client);
			loader.setCommitId(current.id);
			List<JsonNode> nodes = new ArrayList<>();
			App.runWithProgress("Loading data", () -> {
				JsonElement currentElement = loader.getRemoteJson(ref);
				JsonElement previousElement = null;
				String previousCommit = loadPreviousCommit(ref);
				if (previousCommit != null) {
					loader.setCommitId(previousCommit);
					previousElement = loader.getRemoteJson(ref);
				}
				nodes.add(new ModelNodeBuilder().build(currentElement, previousElement));
			});
			diffViewer.setInput(nodes.get(0));
		});
	}

	private String loadPreviousCommit(FetchRequestData ref) {
		RepositoryClient client = Database.getRepositoryClient();
		try {
			return client.getPreviousReference(ref.type, ref.refId, current.id);
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.NOT_FOUND.getStatusCode())
				return null;
			log.warn("Error loading previous commit", e);
			Error.showBox(e.getMessage());
			return null;
		}
	}

	private void loadCommitHistory() {
		RepositoryClient client = Database.getRepositoryClient();
		try {
			historyViewer.setInput(client.fetchCommitHistory());
		} catch (Exception e) {
			log.warn("Error loading commit history", e);
			Error.showBox(e.getMessage());
		}
	}

	public static void refresh() {
		if (instance == null)
			return;
		instance.historyViewer.setInput(new Commit[0]);
		instance.referencesViewer.setInput(new FetchRequestData[0]);
		instance.diffViewer.setInput(null);
		if (Database.isConnected()) {
			instance.loadCommitHistory();
			return;
		}
	}

	@Override
	public void dispose() {
		instance = null;
		super.dispose();
	}

	private void loadReferences(Commit commit) {
		if (!Database.isConnected())
			return;
		RepositoryClient client = Database.getRepositoryClient();
		List<FetchRequestData> references = new ArrayList<>();
		App.runWithProgress("Loading references", () -> {
			try {
				references.addAll(client.getReferences(commit.id));
			} catch (Exception e) {
				log.warn("Error loading commit history", e);
				Error.showBox(e.getMessage());
			}
		});
		for (FetchRequestData data : new ArrayList<>(references))
			if (!data.type.isCategorized())
				references.remove(data);
		referencesViewer.setInput(references);
	}

	@Override
	public void setFocus() {

	}

	private class ReferencesViewer extends
			AbstractTableViewer<FetchRequestData> {

		private FetchRequestData lastSelection;

		private ReferencesViewer(Composite parent) {
			super(parent);
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return new ReferencesLabel();
		}

	}

	private class ReferencesLabel extends BaseLabelProvider {

		@Override
		public String getText(Object element) {
			if (!(element instanceof FetchRequestData))
				return null;
			FetchRequestData data = (FetchRequestData) element;
			return data.fullPath;
		}

		@Override
		public Image getImage(Object element) {
			if (!(element instanceof FetchRequestData))
				return null;
			FetchRequestData data = (FetchRequestData) element;
			Overlay overlay = null;
			if (data.isAdded())
				overlay = Overlay.ADDED;
			else if (data.isDeleted())
				overlay = Overlay.DELETED;
			if (data.type == ModelType.CATEGORY)
				return Images.getForCategory(data.categoryType, overlay);
			return Images.get(data.type, overlay);
		}

	}

}
