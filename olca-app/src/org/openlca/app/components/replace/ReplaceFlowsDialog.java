package org.openlca.app.components.replace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class ReplaceFlowsDialog extends FormDialog {

	private FlowViewer selectionViewer;
	private FlowViewer replacementViewer;
	private Button excludeWithProviders;
	private Button replaceFlowsButton;
	private Button replaceImpactsButton;
	private Button replaceBothButton;

	public static void openDialog() {
		if (Database.get() == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
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
		newShell.setText(M.BulkreplaceFlows);
		newShell.setSize(800, 400);
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
		UI.gridData(top, true, false);
		selectionViewer = createFlowViewer(top, toolkit, M.ReplaceFlow, this::updateReplacementCandidates);
		replacementViewer = createFlowViewer(top, toolkit, M.With, selected -> updateButtons());
		replacementViewer.setEnabled(false);
		selectionViewer.setInput(getUsed());
		toolkit.paintBordersFor(top);
		toolkit.adapt(top);
	}

	private FlowViewer createFlowViewer(Composite parent, FormToolkit toolkit, String label,
			Consumer<FlowDescriptor> onChange) {
		UI.formLabel(parent, toolkit, label);
		FlowViewer viewer = new FlowViewer(parent);
		viewer.addSelectionChangedListener(onChange);
		return viewer;
	}

	private void updateReplacementCandidates(FlowDescriptor selected) {
		List<FlowDescriptor> candidates = getReplacementCandidates(selected);
		replacementViewer.setInput(candidates);
		if (candidates.size() == 1) {
			replacementViewer.select(candidates.get(0));
		}
		replacementViewer.setEnabled(candidates.size() > 1);
		updateButtons();
	}

	private void createBottom(Composite parent, FormToolkit toolkit) {
		Composite bottom = UI.formComposite(parent, toolkit);
		UI.gridLayout(bottom, 1, 0, 0);
		Composite typeContainer = UI.formComposite(bottom, toolkit);
		UI.gridLayout(typeContainer, 4, 20, 5);
		UI.formLabel(typeContainer, toolkit, M.ReplaceIn);
		replaceFlowsButton = toolkit.createButton(typeContainer, M.InputsOutputs, SWT.RADIO);
		replaceFlowsButton.setSelection(true);
		Controls.onSelect(replaceFlowsButton, this::updateSelection);
		replaceImpactsButton = toolkit.createButton(typeContainer, M.ImpactFactors, SWT.RADIO);
		Controls.onSelect(replaceImpactsButton, this::updateSelection);
		replaceBothButton = toolkit.createButton(typeContainer, M.Both, SWT.RADIO);
		Controls.onSelect(replaceBothButton, this::updateSelection);
		Composite excludeContainer = UI.formComposite(bottom, toolkit);
		UI.gridLayout(excludeContainer, 2, 20, 5);
		excludeWithProviders = UI.formCheckbox(excludeContainer, toolkit);
		UI.formLabel(excludeContainer, toolkit, M.ExcludeExchangesWithDefaultProviders);
		toolkit.paintBordersFor(bottom);
		toolkit.adapt(bottom);
		createNote(parent, toolkit);
	}

	private void updateSelection(SelectionEvent e) {
		Button source = (Button) e.getSource();
		if (!source.getSelection())
			return;
		for (Button button : new Button[] { replaceFlowsButton, replaceImpactsButton, replaceBothButton }) {
			if (source == button)
				continue;
			button.setSelection(false);
		}
		excludeWithProviders.setEnabled(source == replaceFlowsButton || source == replaceBothButton);
	}

	private void createNote(Composite parent, FormToolkit toolkit) {
		String note = M.NoteDefaultProviders;
		Label noteLabel = toolkit.createLabel(parent, note, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd.widthHint = 300;
		noteLabel.setLayoutData(gd);
	}

	private void updateButtons() {
		FlowDescriptor first = selectionViewer.getSelected();
		FlowDescriptor second = replacementViewer.getSelected();
		boolean enabled = first != null
				&& first.id != 0L
				&& second != null
				&& second.id != 0L;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	private List<FlowDescriptor> getUsed() {
		FlowDao dao = new FlowDao(Database.get());
		Set<Long> ids = dao.getUsed();
		List<FlowDescriptor> result = new ArrayList<>();
		result.add(new FlowDescriptor());
		result.addAll(dao.getDescriptors(ids));
		return result;
	}

	private List<FlowDescriptor> getReplacementCandidates(FlowDescriptor flow) {
		if (flow == null || flow.id == 0L)
			return Collections.emptyList();
		FlowDao dao = new FlowDao(Database.get());
		Set<Long> ids = dao.getReplacementCandidates(flow.id, flow.flowType);
		List<FlowDescriptor> result = new ArrayList<>();
		result.addAll(dao.getDescriptors(ids));
		result.remove(flow);
		return result;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void okPressed() {
		FlowDescriptor oldFlow = selectionViewer.getSelected();
		FlowDescriptor newFlow = replacementViewer.getSelected();
		FlowDao dao = new FlowDao(Database.get());
		boolean replaceFlows = replaceBothButton.getSelection() || replaceFlowsButton.getSelection();
		boolean replaceImpacts = replaceBothButton.getSelection() || replaceImpactsButton.getSelection();
		if (replaceFlows) {
			if (excludeWithProviders.getSelection()) {
				dao.replaceExchangeFlowsWithoutProviders(oldFlow.id, newFlow.id);
			} else {
				dao.replaceExchangeFlows(oldFlow.id, newFlow.id);
			}
		}
		if (replaceImpacts) {
			dao.replaceImpactFlows(oldFlow.id, newFlow.id);
		}
		Database.get().getEntityFactory().getCache().evictAll();
		super.okPressed();
	}

}
