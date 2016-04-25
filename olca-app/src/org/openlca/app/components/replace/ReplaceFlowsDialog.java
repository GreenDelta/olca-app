package org.openlca.app.components.replace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.util.Info;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.BaseNameSorter;
import org.openlca.core.database.Daos;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class ReplaceFlowsDialog extends FormDialog {

	private List<FlowDescriptor> usedInExchanges;
	private List<FlowDescriptor> replacementCandidates;
	private ComboViewer selectionViewer;
	private ComboViewer replacementViewer;
	private Button excludeWithProviders;

	public static void openDialog() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		new ReplaceFlowsDialog().open();
	}

	public ReplaceFlowsDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("#Mass-replace flows");
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		FormToolkit toolkit = mForm.getToolkit();
		Composite body = UI.formBody(mForm.getForm(), toolkit);
		UI.gridLayout(body, 1, 0, 20);
		createTop(body, toolkit);
		createBottom(body, toolkit);
	}

	private void createTop(Composite parent, FormToolkit toolkit) {
		Composite top = UI.formComposite(parent, toolkit);
		UI.gridLayout(top, 2, 20, 5);
		usedInExchanges = getUsedInExchanges();
		selectionViewer = createSelectionViewer(top, toolkit);
		replacementViewer = createReplacementViewer(top, toolkit);
		toolkit.paintBordersFor(top);
		toolkit.adapt(top);
	}

	private void createBottom(Composite parent, FormToolkit toolkit) {
		Composite bottom = UI.formComposite(parent, toolkit);
		UI.gridLayout(bottom, 2, 20, 5);
		excludeWithProviders = UI.formCheckbox(bottom, toolkit);
		UI.formLabel(bottom, toolkit, "#Exclude exchanges with default providers");
		toolkit.paintBordersFor(bottom);
		toolkit.adapt(bottom);
		createNote(parent, toolkit);
	}

	private void createNote(Composite parent, FormToolkit toolkit) {
		String note = "#Note: Default providers of replaced exchanges will be removed, because existing providers will not match the new replaced flows. Check the box above to prevent the default providers to be removed (Flows will not be replaced in these cases).";
		Label noteLabel = toolkit.createLabel(parent, note, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd.widthHint = 300;
		noteLabel.setLayoutData(gd);
	}

	private ComboViewer createSelectionViewer(Composite parent, FormToolkit toolkit) {
		UI.formLabel(parent, toolkit, "#Replace flow");
		ComboViewer viewer = new ComboViewer(new CCombo(parent, SWT.DROP_DOWN));
		decorateViewer(viewer);
		NameFilter filter = new NameFilter();
		viewer.addFilter(filter);
		viewer.setInput(usedInExchanges);
		UI.gridData(viewer.getCCombo(), true, false).widthHint = 300;
		viewer.getCCombo().addKeyListener(new FilterOnKey(viewer, filter, () -> usedInExchanges));
		viewer.addSelectionChangedListener((e) -> {
			FlowDescriptor selected = Viewers.getFirstSelected(viewer);
			if (selected == null || selected.getId() == 0l) {
				replacementCandidates = new ArrayList<>();
			} else {
				replacementCandidates = getReplacementCandidates(selected);
			}
			replacementViewer.setInput(replacementCandidates);
			updateButtons();
		});
		return viewer;
	}

	private ComboViewer createReplacementViewer(Composite parent, FormToolkit toolkit) {
		UI.formLabel(parent, toolkit, "#with");
		ComboViewer viewer = new ComboViewer(new CCombo(parent, SWT.NONE));
		decorateViewer(viewer);
		NameFilter filter = new NameFilter();
		viewer.addFilter(filter);
		viewer.getCCombo().addKeyListener(new FilterOnKey(viewer, filter, () -> replacementCandidates));
		UI.gridData(viewer.getCCombo(), true, false).widthHint = 300;
		viewer.addSelectionChangedListener((e) -> {
			updateButtons();
		});
		return viewer;
	}

	private void updateButtons() {
		FlowDescriptor first = Viewers.getFirstSelected(selectionViewer);
		FlowDescriptor second = Viewers.getFirstSelected(replacementViewer);
		boolean enabled = first != null && first.getId() != 0l && second != null && second.getId() != 0l;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	private void decorateViewer(ComboViewer viewer) {
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setSorter(new BaseNameSorter());
	}

	private List<FlowDescriptor> getUsedInExchanges() {
		FlowDao dao = (FlowDao) Daos.createCategorizedDao(Database.get(), ModelType.FLOW);
		Set<Long> ids = dao.getUsed();
		List<FlowDescriptor> result = new ArrayList<>();
		result.add(new FlowDescriptor());
		result.addAll(dao.getDescriptors(ids));
		return result;
	}

	private List<FlowDescriptor> getReplacementCandidates(FlowDescriptor flow) {
		FlowDao dao = (FlowDao) Daos.createCategorizedDao(Database.get(), ModelType.FLOW);
		Set<Long> ids = dao.getReplacementCandidates(flow.getId(), flow.getFlowType());
		List<FlowDescriptor> result = new ArrayList<>();
		result.add(new FlowDescriptor());
		result.addAll(dao.getDescriptors(ids));
		return result;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void okPressed() {
		FlowDescriptor oldFlow = Viewers.getFirstSelected(selectionViewer);
		FlowDescriptor newFlow = Viewers.getFirstSelected(replacementViewer);
		FlowDao dao = (FlowDao) Daos.createCategorizedDao(Database.get(), ModelType.FLOW);
		dao.replace(oldFlow.getId(), newFlow.getId(), excludeWithProviders.getSelection());
		super.okPressed();
	}

	private class LabelProvider extends BaseLabelProvider {

		private Category getCategory(FlowDescriptor flow) {
			if (flow == null || flow.getCategory() == null)
				return null;
			return Cache.getEntityCache().get(Category.class, flow.getCategory());
		}

		private String getCategoryText(FlowDescriptor flow) {
			Category category = getCategory(flow);
			if (category == null)
				return "";
			String text = "";
			while (category.getCategory() != null) {
				text = category.getCategory().getName() + "/" + text;
				category = category.getCategory();
			}
			return text;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) element;
			String flowText = Labels.getDisplayName(flow);
			if (flowText == null)
				flowText = "";
			String categoryText = getCategoryText(flow);
			return categoryText + flowText;

		}

	}

}
