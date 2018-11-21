package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.ui.FetchNotifierMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.ModelTypeComparison;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.CredentialSupplier;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.model.data.FileReference;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class RepositoryImportWizard extends Wizard implements IImportWizard {

	private final static Logger log = LoggerFactory.getLogger(RepositoryImportWizard.class);
	private String repo;
	private String user;
	private String pass;
	private RepositoryClient client;
	private Set<FileReference> selection = new HashSet<>();
	private int total;
	private WizardPage currentPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.RepositoryImport);
	}

	@Override
	public void addPages() {
		addPage(new RepositoryWizardPage());
		addPage(new ModelSelectionPage());
	}

	@Override
	public boolean performFinish() {
		Exception e = run((m) -> {
			try {
				if (total == selection.size()) {
					FetchNotifierMonitor monitor = new FetchNotifierMonitor(m, M.DownloadingData);
					client.download(new HashSet<>(), null, monitor);
					return null;
				} else {
					m.beginTask(M.DownloadingData, IProgressMonitor.UNKNOWN);
					File tmp = client.downloadJson(selection);
					m.beginTask(M.ImportData, IProgressMonitor.UNKNOWN);
					ZipStore store = ZipStore.open(tmp);
					JsonImport jsonImport = new JsonImport(store, Database.get());
					jsonImport.run();
					store.close();
					tmp.delete();
					return null;
				}
			} catch (Exception ex) {
				return ex;
			}
		});
		if (e != null) {
			currentPage.setErrorMessage(e.getMessage());
			return false;
		}
		Navigator.refresh();
		return true;
	}

	private boolean initClient() {
		try {
			String url = repo;
			if (url.endsWith("/"))
				url = url.substring(0, url.length() - 1);
			String repoName = url.substring(url.lastIndexOf('/') + 1);
			url = url.substring(0, url.lastIndexOf('/'));
			String groupName = url.substring(url.lastIndexOf('/') + 1);
			String baseUrl = url.substring(0, url.lastIndexOf('/')) + "/ws";
			String repoId = groupName + "/" + repoName;
			CredentialSupplier credentials = getCredentials();
			RepositoryConfig config = new RepositoryConfig(Database.get(), baseUrl, repoId, credentials);
			client = new RepositoryClient(config);
			return true;
		} catch (Exception e) {
			log.error("Error initializing client", e);
			return false;
		}
	}

	private CredentialSupplier getCredentials() {
		if (Strings.isNullOrEmpty(pass) || Strings.isNullOrEmpty(user))
			return null;
		return new CredentialSupplier(user, pass);
	}

	private Exception run(Function<IProgressMonitor, Exception> runnable) {
		try {
			getContainer().run(true, false, (m) -> {
				Exception e = runnable.apply(m);
				if (e != null)
					throw new InvocationTargetException(e, e.getMessage());
			});
			return null;
		} catch (Exception e) {
			if (e.getCause() instanceof WebRequestException)
				return (WebRequestException) e.getCause();
			return e;
		}
	}

	private class RepositoryWizardPage extends WizardPage {

		protected RepositoryWizardPage() {
			super(RepositoryWizardPage.class.getCanonicalName());
			setPageComplete(false);
			setImageDescriptor(Icon.CLOUD_LOGO.descriptor());
			setTitle(M.SelectRepository);
			setDescription(M.PleaseSpecifyCompleteUrl);
		}

		@Override
		public void createControl(Composite parent) {
			Composite container = UI.formComposite(parent);
			setControl(container);
			Text repoText = UI.formText(container, M.URL + "*");
			repoText.addModifyListener((e) -> {
				repo = repoText.getText();
				checkCompletion();
			});
			Text userText = UI.formText(container, M.User);
			userText.addModifyListener((e) -> user = userText.getText());
			Text passText = UI.formText(container, M.Password, SWT.PASSWORD);
			passText.addModifyListener((e) -> pass = passText.getText());
		}

		private void checkCompletion() {
			setPageComplete(!Strings.isNullOrEmpty(repo));
		}

		@Override
		public void setVisible(boolean visible) {
			if (visible) {
				currentPage = this;
			}
			super.setVisible(visible);
		}

	}

	private class ModelSelectionPage extends WizardPage {

		private CheckboxTreeViewer viewer;
		private Node root;

		protected ModelSelectionPage() {
			super(ModelSelectionPage.class.getCanonicalName());
			setPageComplete(false);
			setImageDescriptor(Icon.CLOUD_LOGO.descriptor());
			setTitle(M.SelectDataSets);
			setDescription(M.PleaseSelectElements);
		}

		@Override
		public void createControl(Composite parent) {
			Composite container = UI.formComposite(parent);
			setControl(container);
			viewer = new CheckboxTreeViewer(container, SWT.BORDER);
			viewer.setLabelProvider(new Label());
			viewer.setContentProvider(new ContentProvider());
			viewer.setUseHashlookup(true);
			viewer.addCheckStateListener(new SelectionState(this));
			UI.gridData(viewer.getTree(), true, true);
		}

		@Override
		public void setVisible(boolean visible) {
			if (!visible) {
				super.setVisible(false);
				return;
			}
			currentPage = this;
			boolean initialized = initClient();
			if (!initialized) {
				setErrorMessage(M.CouldNotConnect);
				super.setVisible(visible);
				return;
			}
			setErrorMessage(null);
			Exception e = run(this::scanRepository);
			if (e != null) {
				setErrorMessage(e.getMessage());
			} else {
				viewer.setInput(root);
			}
			super.setVisible(visible);
		}

		private Exception scanRepository(IProgressMonitor m) {
			try {
				m.beginTask(M.ScanningRepository, IProgressMonitor.UNKNOWN);
				Set<FetchRequestData> data = client.list();
				List<FetchRequestData> actualData = new ArrayList<>();
				data.forEach(d -> {
					if (d.type.isCategorized()) {
						actualData.add(d);
					}
				});
				total = data.size();
				Collections.sort(actualData, new DataComparator());
				Map<String, Node> categoryNodes = new HashMap<>();
				root = new Node();
				for (FetchRequestData frd : actualData) {
					Node parent = null;
					if (frd.categories != null && frd.categories.size() > 0) {
						parent = categoryNodes.get(toCategoryKey(frd));
					} else if (frd.type == ModelType.CATEGORY) {
						parent = getTypeNode(root, frd.categoryType, categoryNodes);
					} else {
						parent = getTypeNode(root, frd.type, categoryNodes);
					}
					Node node = new Node(parent, frd);
					if (frd.type != ModelType.CATEGORY)
						continue;
					String key = frd.categoryType.name();
					if (frd.categories != null && frd.categories.size() > 0) {
						key += "/" + org.openlca.util.Strings.join(frd.categories, '/');
					}
					key += "/" + frd.name;
					categoryNodes.put(key, node);
				}
				m.done();
				return null;
			} catch (Exception ex) {
				return ex;
			}
		}

		private Node getTypeNode(Node root, ModelType type, Map<String, Node> categoryNodes) {
			Node node = categoryNodes.get(type.name());
			if (node != null)
				return node;
			node = new Node(root, type);
			categoryNodes.put(type.name(), node);
			return node;
		}

		private String toCategoryKey(FetchRequestData data) {
			String path = data.type.name();
			if (data.type == ModelType.CATEGORY)
				path = data.categoryType.name();
			if (data.categories == null || data.categories.size() == 0)
				return path;
			return path + "/" + org.openlca.util.Strings.join(data.categories, '/');
		}

		private void checkCompletion() {
			setPageComplete(!selection.isEmpty());
		}

	}

	private class DataComparator implements Comparator<FetchRequestData> {

		@Override
		public int compare(FetchRequestData o1, FetchRequestData o2) {
			if (o1.type != o2.type)
				return ModelTypeComparison.compare(o1.type, o2.type);
			if (o1.type == ModelType.CATEGORY && o1.categoryType != o2.categoryType)
				return ModelTypeComparison.compare(o1.categoryType, o2.categoryType);
			return CloudUtil.toFullPath(o1).toLowerCase().compareTo(CloudUtil.toFullPath(o2).toLowerCase());
		}

	}

	private class Node {

		private final Node parent;
		private final ModelType type;
		private final FetchRequestData data;
		private final List<Node> children = new ArrayList<>();

		private Node() {
			this(null, null, null);
		}

		private Node(Node parent, ModelType type) {
			this(parent, null, type);
		}

		private Node(Node parent, FetchRequestData data) {
			this(parent, data, null);
		}

		private Node(Node parent, FetchRequestData data, ModelType type) {
			this.parent = parent;
			this.data = data;
			this.type = type;
			if (parent != null) {
				parent.children.add(this);
			}
		}

	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			Node node = (Node) inputElement;
			return node.children.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			Node node = (Node) parentElement;
			return node.children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			Node node = (Node) element;
			return node.parent;
		}

		@Override
		public boolean hasChildren(Object element) {
			Node node = (Node) element;
			return !node.children.isEmpty();
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private class Label extends BaseLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			Node node = (Node) element;
			if (node.data == null)
				return Images.getForCategory(node.type);
			if (node.data.type == ModelType.CATEGORY)
				return Images.getForCategory(node.data.categoryType);
			return Images.get(node.data.type);
		}

		@Override
		public String getText(Object element) {
			Node node = (Node) element;
			if (node.data == null)
				return Labels.modelType(node.type);
			return node.data.name;
		}

	}

	private class SelectionState implements ICheckStateListener {

		private final ModelSelectionPage page;
		private final CheckboxTreeViewer viewer;

		public SelectionState(ModelSelectionPage page) {
			this.page = page;
			this.viewer = page.viewer;
		}

		private void updateChildren(Node element, boolean state) {
			for (Node child : element.children) {
				viewer.setGrayed(child, false);
				viewer.setChecked(child, state);
				updateSelection(child, state);
				if (child.data.type == ModelType.CATEGORY) {
					updateChildren(child, state);
				}
			}
		}

		private void updateParent(Node element) {
			Node parent = element.parent;
			if (parent == null)
				return;
			boolean checked = false;
			boolean all = true;
			for (Node child : parent.children) {
				checked = viewer.getChecked(child) || viewer.getGrayed(child);
				if (!viewer.getChecked(child) || viewer.getGrayed(child))
					all = false;
			}
			viewer.setGrayed(parent, !all && checked);
			viewer.setChecked(parent, checked);
			updateSelection(parent, checked);
			updateParent(parent);
		}

		private void updateSelection(Node element, boolean selected) {
			if (element.data == null)
				return;
			if (selected)
				selection.add(toFileReference(element.data));
			else
				selection.remove(toFileReference(element.data));
			page.checkCompletion();
		}

		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			viewer.getControl().setRedraw(false);
			Node element = (Node) event.getElement();
			viewer.setGrayed(element, false);
			updateChildren(element, event.getChecked());
			updateParent(element);
			if (element.data != null) {
				updateSelection(element, event.getChecked());
			}
			viewer.getControl().setRedraw(true);
		}

		private FileReference toFileReference(FetchRequestData data) {
			FileReference ref = new FileReference();
			ref.type = data.type;
			ref.refId = data.refId;
			return ref;
		}

	}

}
