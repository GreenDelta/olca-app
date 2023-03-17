package org.openlca.app.results.grouping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ProcessGroupSetDao;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessGroup;
import org.openlca.core.model.ProcessGroupSet;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ProcessGrouping;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The page of the analysis editor with the grouping function.
 */
public class GroupPage extends FormPage {

	final ResultEditor editor;

	List<ProcessGrouping> groups;
	ProcessGroupSet groupSet;

	private TableViewer groupViewer;
	private TableViewer processViewer;
	private Menu groupMoveMenu;
	private GroupResultSection resultSection;
	private Section groupingSection;

	public GroupPage(ResultEditor editor) {
		super(editor, "analysis.GroupPage", M.Grouping);
		this.editor = editor;
		initGroups();
	}

	private void initGroups() {
		groups = new ArrayList<>();
		var restGroup = new ProcessGrouping();
		restGroup.name = M.Other;
		restGroup.rest = true;
		restGroup.processes.addAll(editor.items.providers());
		groups.add(restGroup);
	}

	public void applyGrouping(ProcessGroupSet groupSet) {
		if (groupSet == null || editor.result == null)
			return;
		this.groupSet = groupSet;
		var newGroups = ProcessGrouping.applyOn(
				editor.items.providers(), groupSet, M.Other);
		groups.clear();
		groups.addAll(newGroups);
		updateViewers();
		updateTitle();
	}

	private void updateViewers() {
		if (groupViewer != null && processViewer != null
				&& resultSection != null) {
			groupViewer.refresh(true);
			processViewer.setInput(Collections.emptyList());
			resultSection.update();
		}
	}

	void updateTitle() {
		if (groupingSection == null)
			return;
		// Add the current group set name to the section title, if it is not null.
		var title = groupSet != null
				? M.Groups + " (" + groupSet.name + ")"
				: M.Groups;
		groupingSection.setText(title);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform,
				Labels.name(editor.setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var toolkit = mform.getToolkit();
		var body = UI.body(form, toolkit);
		createGroupingSection(toolkit, body);
		resultSection = new GroupResultSection(groups, editor);
		resultSection.render(body, toolkit);
		form.reflow(true);
	}

	private void createGroupingSection(FormToolkit toolkit, Composite body) {
		groupingSection = UI.section(body, toolkit, M.Groups);
		Composite composite = UI.sectionClient(groupingSection, toolkit);
		UI.gridLayout(composite, 2);
		Actions.bind(groupingSection, new AddGroupAction(),
				new SaveGroupSetAction(this), new GroupSetAction(this));
		createGroupViewer(composite);
		processViewer = Tables.createViewer(composite);
		UI.gridData(processViewer.getControl(), true, false).heightHint = 200;
		configureViewer(processViewer);
		createMoveMenu();
	}

	private void createGroupViewer(Composite composite) {
		groupViewer = Tables.createViewer(composite);
		GridData groupData = UI.gridData(groupViewer.getControl(), false, false);
		groupData.heightHint = 200;
		groupData.widthHint = 250;
		configureViewer(groupViewer);
		groupViewer.setInput(groups);
		Actions.bind(groupViewer, new AddGroupAction(), new DeleteGroupAction());
		groupViewer.addSelectionChangedListener(e -> {
			ProcessGrouping g = Selections.firstOf(e);
			if (g != null)
				processViewer.setInput(g.processes);
		});
	}

	private void configureViewer(TableViewer viewer) {
		viewer.setLabelProvider(new GroupPageLabel());
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setComparator(new GroupPageComparator());
	}

	private void createMoveMenu() {
		Menu menu = new Menu(processViewer.getTable());
		processViewer.getTable().setMenu(menu);
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText(M.Move);
		groupMoveMenu = new Menu(item);
		item.setMenu(groupMoveMenu);
		groupMoveMenu.addListener(SWT.Show, new MenuGroupListener());
	}

	private class AddGroupAction extends Action {

		public AddGroupAction() {
			setImageDescriptor(Icon.ADD.descriptor());
			setText(M.Add);
			setToolTipText(M.Add);
		}

		@Override
		public void run() {
			String m = M.PleaseEnterAName;
			InputDialog dialog = new InputDialog(getSite().getShell(), m, m,
					"", null);
			int code = dialog.open();
			if (code == Window.OK) {
				String name = dialog.getValue();
				ProcessGrouping group = new ProcessGrouping();
				group.name = name;
				groups.add(group);
				groupViewer.add(group);
				resultSection.update();
				updateViewers();
			}
		}
	}

	private class DeleteGroupAction extends Action {

		public DeleteGroupAction() {
			setImageDescriptor(Icon.DELETE.descriptor());
			setText(M.Delete);
		}

		@Override
		public void run() {
			ProcessGrouping grouping = Viewers.getFirstSelected(groupViewer);
			if (grouping == null || grouping.rest)
				return;
			ProcessGrouping rest = findRest();
			if (rest == null)
				return;
			groups.remove(grouping);
			rest.processes.addAll(grouping.processes);
			updateViewers();
		}

		private ProcessGrouping findRest() {
			if (groups == null)
				return null;
			for (ProcessGrouping g : groups) {
				if (g.rest)
					return g;
			}
			return null;
		}
	}

	private class MenuGroupListener implements Listener, SelectionListener,
			Comparator<ProcessGrouping> {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		/**
		 * Executed when an item is selected: moves processes to a group.
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			Object o = e.widget.getData();
			if (!(o instanceof ProcessGrouping targetGroup))
				return;
			ProcessGrouping sourceGroup = Viewers.getFirstSelected(groupViewer);
			if (sourceGroup == null)
				return;
			List<ProcessDescriptor> processes = Viewers
					.getAllSelected(processViewer);
			if (processes.isEmpty())
				return;
			move(sourceGroup, targetGroup, processes);
		}

		private void move(ProcessGrouping sourceGroup,
				ProcessGrouping targetGroup,
				List<ProcessDescriptor> processes) {
			sourceGroup.processes.removeAll(processes);
			targetGroup.processes.addAll(processes);
			processViewer.setInput(sourceGroup.processes);
			resultSection.update();
		}

		/**
		 * Executed when the menu is shown: fills the group-menu
		 */
		@Override
		public void handleEvent(Event event) {
			ProcessGrouping group = Viewers.getFirstSelected(groupViewer);
			if (group == null)
				return;
			for (MenuItem item : groupMoveMenu.getItems()) {
				item.removeSelectionListener(this);
				item.dispose();
			}
			List<ProcessGrouping> other = getOther(group);
			for (ProcessGrouping g : other) {
				MenuItem menuItem = new MenuItem(groupMoveMenu, SWT.PUSH);
				menuItem.setText(g.name);
				menuItem.setData(g);
				menuItem.addSelectionListener(this);
			}
		}

		private List<ProcessGrouping> getOther(ProcessGrouping group) {
			List<ProcessGrouping> other = new ArrayList<>();
			for (ProcessGrouping g : groups) {
				if (g.equals(group))
					continue;
				other.add(g);
			}
			other.sort(this);
			return other;
		}

		@Override
		public int compare(ProcessGrouping o1, ProcessGrouping o2) {
			if (o1 == null || o2 == null)
				return 0;
			return Strings.compare(o1.name, o2.name);
		}
	}

	private static class GroupPageLabel extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			if (element instanceof ProcessGrouping)
				return Icon.FOLDER_BLUE.get();
			return Images.get(FlowType.PRODUCT_FLOW);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ProcessGrouping group) {
				return group.name;
			} else if (element instanceof ProcessDescriptor p) {
				return Labels.name(p);
			} else
				return null;
		}
	}

	/**
	 * A viewer comparator for groups and processes on the grouping page.
	 */
	private static class GroupPageComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object first, Object second) {
			if ((first instanceof ProcessGrouping)
					&& (second instanceof ProcessGrouping))
				return compareGroups((ProcessGrouping) first,
						(ProcessGrouping) second);
			if ((first instanceof ProcessDescriptor)
					&& (second instanceof ProcessDescriptor))
				return compareProcesses((ProcessDescriptor) first,
						(ProcessDescriptor) second);
			return 0;
		}

		private int compareProcesses(ProcessDescriptor first,
				ProcessDescriptor second) {
			return compareNames(first.name, second.name);
		}

		private int compareGroups(ProcessGrouping first,
				ProcessGrouping second) {
			return compareNames(first.name, second.name);
		}

		private int compareNames(String first, String second) {
			if (first == null)
				return -1;
			if (second == null)
				return 1;
			return first.compareToIgnoreCase(second);
		}

	}

	/**
	 * Action for saving a group set in the grouping page of the analysis
	 * editor.
	 */
	private static class SaveGroupSetAction extends Action {

		private final Logger log = LoggerFactory.getLogger(getClass());
		private final GroupPage page;

		public SaveGroupSetAction(GroupPage page) {
			this.page = page;
			setToolTipText(M.Save);
			setImageDescriptor(Icon.SAVE.descriptor());
		}

		@Override
		public void run() {
			ProcessGroupSet groupSet = page.groupSet;
			if (groupSet == null)
				insertNew();
			else
				updateExisting(groupSet);
		}

		private void insertNew() {
			ProcessGroupSet groupSet;
			try {
				groupSet = createGroupSet();
				if (groupSet == null)
					return;
				setAndSaveGroups(groupSet);
				page.groupSet = groupSet;
				page.updateTitle();
			} catch (Exception e) {
				log.error("Failed to save process group set", e);
			}
		}

		private ProcessGroupSet createGroupSet() {
			Shell shell = page.getEditorSite().getShell();
			InputDialog dialog = new InputDialog(shell, M.SaveAs,
					M.PleaseEnterAName, "", null);
			int code = dialog.open();
			if (code == Window.CANCEL)
				return null;
			ProcessGroupSet set = new ProcessGroupSet();
			set.name = dialog.getValue();
			new ProcessGroupSetDao(Database.get()).insert(set);
			return set;
		}

		private void updateExisting(ProcessGroupSet groupSet) {
			try {
				boolean b = Question.ask(M.SaveChanges,
						M.SaveChangesQuestion);
				if (b)
					setAndSaveGroups(groupSet);
			} catch (Exception e) {
				log.error("Failed to save process group set", e);
			}
		}

		private List<ProcessGroup> createGroups() {
			List<ProcessGrouping> pageGroups = page.groups;
			if (pageGroups == null)
				return Collections.emptyList();
			List<ProcessGroup> groups = new ArrayList<>();
			for (ProcessGrouping pageGroup : pageGroups) {
				if (pageGroup.rest)
					continue;
				ProcessGroup group = new ProcessGroup();
				group.name = pageGroup.name;
				groups.add(group);
				for (var p : pageGroup.processes)
					group.processIds.add(p.refId);
			}
			return groups;
		}

		private void setAndSaveGroups(ProcessGroupSet groupSet)
				throws Exception {
			List<ProcessGroup> groups = createGroups();
			groupSet.setGroups(groups);
			new ProcessGroupSetDao(Database.get()).update(groupSet);
			page.groupSet = groupSet;
		}
	}

}
