package org.openlca.app.cloud.ui.commits;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.App;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.ModelLabelProvider;
import org.openlca.app.cloud.ui.compare.ModelNodeBuilder;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Colors;
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

public class HistoryView extends ViewPart {

	private static HistoryView instance;
	private final static Logger log = LoggerFactory.getLogger(HistoryView.class);
	private HistoryViewer historyViewer;
	private ReferencesViewer referencesViewer;
	private JsonTreeViewer jsonViewer;

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
			if (commit.equals(historyViewer.lastSelection))
				return;
			historyViewer.lastSelection = commit;
			loadReferences(commit);
		});
	}

	private void createJsonViewer(Composite parent) {
		jsonViewer = new JsonTreeViewer(parent, Side.RIGHT, null);
		jsonViewer.setLabelProvider(new ModelLabelProvider());
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
			List<JsonNode> nodes = new ArrayList<>();
			App.runWithProgress("Loading data", () -> {
				nodes.add(new ModelNodeBuilder().build(null, loader.getRemoteJson(ref)));
			});
			jsonViewer.setInput(nodes.toArray(new JsonNode[nodes.size()]));
		});
	}

	private void loadCommitHistory() {
		RepositoryClient client = Database.getRepositoryClient();
		try {
			historyViewer.setInput(client.fetchCommitHistory());
		} catch (WebRequestException e) {
			log.error("Error loading commit history", e);
		}
	}

	public static void refresh() {
		if (instance == null)
			return;
		if (Database.isConnected()) {
			instance.loadCommitHistory();
			return;
		}
		instance.historyViewer.setInput(new Commit[0]);
		instance.referencesViewer.setInput(new FetchRequestData[0]);
		instance.jsonViewer.setInput(new JsonNode[0]);
	}

	private void loadReferences(Commit commit) {
		if (!Database.isConnected())
			return;
		RepositoryClient client = Database.getRepositoryClient();
		List<FetchRequestData> references = new ArrayList<>();
		App.runWithProgress("Loading references", () -> {
			try {
				references.addAll(client.getReferences(commit.id));
			} catch (WebRequestException e) {
				log.error("Error loading commit history", e);
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

	private class HistoryViewer extends AbstractTableViewer<Commit> {

		private Commit lastSelection;

		private HistoryViewer(Composite parent) {
			super(parent);
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return new HistoryLabel();
		}

		@Override
		protected String[] getColumnHeaders() {
			return new String[] { "#Id", "#Message", "#Committer", "#Committed date" };
		}

	}

	private class HistoryLabel extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider,
			ITableColorProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int column) {
			Commit commit = (Commit) element;
			switch (column) {
			case 0:
				return commit.id;
			case 1:
				return commit.message;
			case 2:
				return commit.user;
			case 3:
				return CloudUtil.formatCommitDate(commit.timestamp);
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int column) {
			Commit commit = (Commit) element;
			String lastCommitId = Database.getRepositoryClient().getConfig().getLastCommitId();
			if (!commit.id.equals(lastCommitId))
				return null;
			return Colors.get(112, 179, 89);
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

	}

	private class ReferencesViewer extends AbstractTableViewer<FetchRequestData> {

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
