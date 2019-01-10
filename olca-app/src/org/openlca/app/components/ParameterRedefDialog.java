package org.openlca.app.components;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for the creation of parameter redefinitions from existing global or
 * process parameters.
 */
public class ParameterRedefDialog extends FormDialog {

	private TreeModel model;
	private Text filterText;
	private TreeViewer viewer;
	private IStructuredSelection selection;

	/**
	 * Opens the dialog which contains all global parameters and parameters of
	 * processes and LCIA methods which IDs are contained in the set of valid
	 * contexts.
	 */
	public static List<ParameterRedef> select(Set<Long> validContexts) {
		TreeModel model = loadModel(Database.get(), Cache.getEntityCache(),
				validContexts);
		ParameterRedefDialog dialog = new ParameterRedefDialog(UI.shell(),
				model);
		if (dialog.open() != OK)
			return Collections.emptyList();
		else
			return dialog.getSelection();
	}

	private static TreeModel loadModel(IDatabase database, EntityCache cache,
			Set<Long> validContexts) {
		try (Connection con = database.createConnection()) {
			List<ParameterRedef> parameters = new ArrayList<>();
			String query = "select * from tbl_parameters where is_input_param = 1";
			ResultSet results = con.createStatement().executeQuery(query);
			while (results.next()) {
				ParameterRedef redef = fetchRedef(results);
				if (redef.contextId == null
						|| validContexts.contains(redef.contextId))
					parameters.add(redef);
			}
			results.close();
			return buildModel(parameters, cache);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ParameterRedefDialog.class);
			log.error("Failed to load parameter from database");
			return new TreeModel();
		}
	}

	private static ParameterRedef fetchRedef(ResultSet results)
			throws Exception {
		ParameterRedef redef = new ParameterRedef();
		redef.name = results.getString("name");
		redef.value = results.getDouble("value");
		long modelId = results.getLong("f_owner");
		if (!results.wasNull()) {
			redef.contextId = modelId;
		}
		return redef;
	}

	private static TreeModel buildModel(List<ParameterRedef> parameters,
			EntityCache cache) {
		TreeModel model = new TreeModel();
		HashMap<Long, ModelNode> createdNodes = new HashMap<>();
		for (ParameterRedef redef : parameters) {
			ParameterNode paramNode = new ParameterNode();
			paramNode.parameter = redef;
			Long modelId = redef.contextId;
			if (modelId == null)
				model.globalParameters.add(paramNode);
			else {
				ModelNode node = createdNodes.get(modelId);
				if (node == null) {
					node = new ModelNode();
					node.model = getModel(modelId, cache);
					createdNodes.put(modelId, node);
				}
				if (node.model != null) {
					redef.contextType = node.model.type;
				}
				paramNode.modelNode = node;
				node.parameters.add(paramNode);
			}
		}
		model.modelNodes.addAll(createdNodes.values());
		return model;
	}

	private static BaseDescriptor getModel(long modelId, EntityCache cache) {
		BaseDescriptor model = cache.get(ImpactMethodDescriptor.class, modelId);
		if (model != null)
			return model;
		else
			return cache.get(ProcessDescriptor.class, modelId);
	}

	private ParameterRedefDialog(Shell shell, TreeModel model) {
		super(shell);
		this.model = model;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		// TODO: calculate from screen size
		return new Point(600, 600);
	}

	private List<ParameterRedef> getSelection() {
		List<Object> list = Viewers.getAll(selection);
		if (list == null)
			return Collections.emptyList();
		List<ParameterRedef> selection = new ArrayList<>();
		for (Object element : list) {
			if (element instanceof ParameterNode) {
				ParameterNode node = (ParameterNode) element;
				selection.add(node.parameter);
			}
		}
		return selection;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point loc = super.getInitialLocation(initialSize);
		int marginTop = (getParentShell().getSize().y - initialSize.y) / 3;
		if (marginTop < 0)
			marginTop = 0;
		return new Point(loc.x, loc.y + marginTop);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit toolkit = mform.getToolkit();
		UI.formHeader(mform, M.SearchParameters);
		Composite body = UI.formBody(mform.getForm(), mform.getToolkit());
		UI.gridLayout(body, 1);
		Label filterLabel = UI.formLabel(body, toolkit, M.Filter);
		filterLabel.setFont(UI.boldFont());
		filterText = UI.formText(body, SWT.SEARCH);
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				viewer.refresh();
			}
		});

		Section section = UI.section(body, toolkit, M.Parameters);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit, 1);
		createViewer(composite);
	}

	private void createViewer(Composite composite) {
		viewer = new TreeViewer(composite);
		UI.gridData(viewer.getTree(), true, true);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(model);
		viewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				return filterNode(parentElement, element);
			}
		});
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return compareNodes(e1, e2);
			}
		});
		viewer.addSelectionChangedListener((event) -> {
			selection = (IStructuredSelection) event.getSelection();
		});
	}

	private int compareNodes(Object e1, Object e2) {
		if (e1 instanceof ParameterNode) {
			ParameterNode node1 = (ParameterNode) e1;
			if (e2 instanceof ParameterNode) {
				ParameterNode node2 = (ParameterNode) e2;
				return Strings.compare(
						node1.parameter.name,
						node2.parameter.name);
			}
			return -1; // global parameters before processes
		}
		if (e1 instanceof ModelNode) {
			ModelNode node1 = (ModelNode) e1;
			if (e2 instanceof ModelNode) {
				ModelNode node2 = (ModelNode) e2;
				return Strings.compare(
						node1.model.name,
						node2.model.name);
			}
			return 1; // process nodes after global parameters
		}
		return 0;
	}

	private boolean filterNode(Object parent, Object element) {
		if (element instanceof TreeModel)
			return true;
		String text = filterText.getText();
		if (text == null || text.trim().isEmpty())
			return true;
		String term = text.trim();
		if (element instanceof ParameterNode)
			return filterParameter(element, term);
		if (element instanceof ModelNode)
			return filterProcess(element, term);
		return false;
	}

	private boolean filterParameter(Object element, String term) {
		ParameterNode node = (ParameterNode) element;
		if (node.modelNode != null && filterNode(null, node.modelNode))
			return true;
		return contains(node, term);
	}

	private boolean filterProcess(Object element, String term) {
		ModelNode node = (ModelNode) element;
		BaseDescriptor model = node.model;
		if (StringUtils.containsIgnoreCase(model.name, term))
			return true;
		for (ParameterNode param : node.parameters)
			if (contains(param, term))
				return true;
		return false;
	}

	private boolean contains(ParameterNode node, String term) {
		ParameterRedef redef = node.parameter;
		return StringUtils.containsIgnoreCase(redef.name, term);
	}

	private static class TreeModel {
		private List<ModelNode> modelNodes = new ArrayList<>();
		private List<ParameterNode> globalParameters = new ArrayList<>();
	}

	private static class ModelNode {
		private BaseDescriptor model;
		private List<ParameterNode> parameters = new ArrayList<>();
	}

	private static class ParameterNode {
		private ParameterRedef parameter;
		private ModelNode modelNode;
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
			if (!(inputElement instanceof TreeModel))
				return new Object[0];
			TreeModel model = (TreeModel) inputElement;
			List<Object> elements = new ArrayList<>();
			elements.addAll(model.globalParameters);
			elements.addAll(model.modelNodes);
			return elements.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ModelNode))
				return null;
			ModelNode node = (ModelNode) parentElement;
			return node.parameters.toArray();
		}

		@Override
		public Object getParent(Object element) {
			if (!(element instanceof ParameterNode))
				return null;
			ParameterNode node = (ParameterNode) element;
			return node.modelNode;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof ModelNode;
		}

	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public Image getImage(Object element) {
			if (element instanceof ParameterNode)
				return Images.get(ModelType.PARAMETER);
			if (element instanceof ModelNode) {
				ModelNode node = (ModelNode) element;
				return Images.get(node.model);
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ModelNode) {
				ModelNode node = (ModelNode) element;
				return Labels.getDisplayName(node.model);
			}
			if (element instanceof ParameterNode) {
				ParameterNode node = (ParameterNode) element;
				return node.parameter.name;
			}
			return null;
		}
	}

}
