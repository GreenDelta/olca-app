package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.app.viewers.combo.UnitViewer;
import org.openlca.core.database.SocialIndicatorDao;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class SocialAspectsPage extends ModelPage<Process> {

	private ProcessEditor editor;

	private TreeViewer tree;
	private TreeModel treeModel = new TreeModel();

	private List<SocialAspect> socialAspects = new ArrayList<>();

	private class SocialAspect extends AbstractEntity {
		SocialIndicator indicator;
		String rawAmount;
		String comment;
		Source source;
	}

	public SocialAspectsPage(ProcessEditor editor) {
		super(editor, "SocialAspectsPage", "#Social aspects");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "#Social aspects");
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createActivitySection(tk, body);
		createEntrySection(tk, body);
	}

	private void createActivitySection(FormToolkit tk, Composite body) {
		Composite comp = UI.formSection(body, tk, "#Activity variable");
		UI.formLabel(comp, tk, "#Quantity");
		FlowPropertyViewer propViewer = new FlowPropertyViewer(comp);
		propViewer.setInput(Database.get());
		UI.formLabel(comp, tk, Messages.Unit);
		UnitViewer unitViewer = new UnitViewer(comp);
		UI.formText(comp, tk, Messages.Amount);
	}

	private void createEntrySection(FormToolkit tk, Composite body) {
		Section section = UI.section(body, tk, "#Social assessment");
		Composite comp = UI.sectionClient(section, tk);
		UI.gridData(section, true, true);
		UI.gridLayout(comp, 1);
		tree = new TreeViewer(comp);
		UI.gridData(tree.getTree(), true, true);
		tree.setContentProvider(new ContentProvider());
		tree.setInput(treeModel);
		Actions.bind(section, Actions.onAdd(this::addIndicator));
	}

	private void addIndicator() {
		BaseDescriptor[] list = ModelSelectionDialog
				.multiSelect(ModelType.SOCIAL_INDICATOR);
		for (BaseDescriptor d : list) {
			boolean found = false;
			for (SocialAspect a : socialAspects) {
				SocialIndicator i = a.indicator;
				if (i != null && i.getId() == d.getId()) {
					found = true;
					break;
				}
			}
			if (!found)
				addIndicator(d);
		}
	}

	private void addIndicator(BaseDescriptor d) {
		SocialIndicatorDao dao = new SocialIndicatorDao(Database.get());
		SocialIndicator i = dao.getForId(d.getId());
		SocialAspect a = new SocialAspect();
		a.indicator = i;
		socialAspects.add(a);
		treeModel.addAspect(a);
		tree.refresh();
	}

	private void deleteIndicator() {

	}

	private class TreeModel {

		CategoryNode root = new CategoryNode();

		void addAspect(SocialAspect a) {
			if (a == null || a.indicator == null)
				return;
			SocialIndicator i = a.indicator;
			CategoryNode n = getNode(i.getCategory());
			n.aspects.add(a);
		}

		CategoryNode getNode(Category c) {
			if (c == null)
				return root;
			CategoryNode parent = getNode(c.getParentCategory());
			CategoryNode node = parent.findChild(c);
			if (node == null) {
				node = new CategoryNode(c);
				parent.childs.add(node);
			}
			return node;
		}

	}

	private class CategoryNode {

		Category category;
		List<CategoryNode> childs = new ArrayList<>();
		List<SocialAspect> aspects = new ArrayList<>();

		CategoryNode() {
		}

		CategoryNode(Category c) {
			category = c;
		}

		private CategoryNode findChild(Category c) {
			for (CategoryNode child : childs) {
				if (Objects.equals(c, child.category))
					return child;
			}
			return null;
		}
	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput,
				Object newInput) {
		}

		@Override
		public Object[] getElements(Object obj) {
			if (!(obj instanceof TreeModel))
				return null;
			TreeModel tm = (TreeModel) obj;
			return getChildren(tm.root);
		}

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof CategoryNode))
				return null;
			CategoryNode cn = (CategoryNode) obj;
			List<Object> list = new ArrayList<>();
			list.addAll(cn.childs);
			list.addAll(cn.aspects);
			return list.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object obj) {
			return obj instanceof CategoryNode;
		}

	}
}
