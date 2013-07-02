/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.process;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.Messages;
import org.openlca.core.application.evaluation.EvaluationController;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.editors.ParameterizableModelEditorWithPropertyPage;
import org.openlca.core.model.Process;
import org.openlca.ui.UI;

/**
 * Editor for processes.
 */
public class ProcessEditor extends ParameterizableModelEditorWithPropertyPage {

	public static String ID = "org.openlca.core.editors.process.ProcessEditor";
	private ExchangePage inputOutputPage;
	private int ioPageIndex;

	public ProcessEditor() {
		super(Messages.Processes_FormText);
	}

	@Override
	protected IPropertySheetPage getPropertySheetPage() {
		return new ExchangePropertiesPage((Process) getModelComponent());
	}

	@Override
	protected void initEvaluationController() {
		super.initEvaluationController();
		EvaluationController controller = getEvaluationController();
		Process process = (Process) getModelComponent();
		controller.resisterProcess(process);
	}

	@Override
	protected void addPages() {
		super.addPages();
		try {
			addPage(new ProcessCostPage(this));
		} catch (Exception e) {
			log.error("Failed to add cost page", e);
		}
	}

	@Override
	protected ModelEditorPage[] initPages() {
		inputOutputPage = new ExchangePage(this);
		getEvaluationController().addEvaluationListener(inputOutputPage);
		ioPageIndex = 1;
		return new ModelEditorPage[] { new ProcessInfoPage(this),
				inputOutputPage,
				new AdminInfoPage(this, (Process) getModelComponent()),
				new ModelingAndValidationPage(this) };
	}

	@Override
	public void dispose() {
		getEvaluationController().removeEvaluationListener(inputOutputPage);
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		final PropertySheet properties = (PropertySheet) PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView(IPageLayout.ID_PROP_SHEET);
		if (properties == null && newPageIndex == ioPageIndex) {
			if (openProperties()) {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage()
							.showView(IPageLayout.ID_PROP_SHEET);
				} catch (final PartInitException e) {
					log.error("Changing page failed", e);
				}
			}

		} else if (newPageIndex == ioPageIndex && properties != null) {
			// set outline of graph editor
			properties.partBroughtToTop(inputOutputPage);
		} else if (properties != null) {
			if (getActivePageInstance() != null) {
				// set outline null (other active page instances does not have
				// an outline
				properties.partBroughtToTop(getActivePageInstance());
			}
		}

	}

	/**
	 * Asks the user if the properties view should be opened.
	 * 
	 * @return True if the user answered 'Yes', false otherwise
	 */
	private boolean openProperties() {
		final PreferenceStore pfStore = ApplicationProperties
				.getPreferenceStore();
		boolean open = false;
		if (!pfStore.contains("openProperties")) {
			final MessageDialogWithToggle toggle = MessageDialogWithToggle
					.openYesNoQuestion(UI.shell(),
							Messages.Processes_PropertiesView,
							Messages.Processes_OpenQuestion,
							Messages.Processes_Remember, false, pfStore,
							"openProperties");
			open = toggle.getReturnCode() == IDialogConstants.YES_ID;
			try {
				pfStore.save();
			} catch (final IOException e) {
				log.error("Open properties failed", e);
			}
		} else {
			open = pfStore.getString("openProperties").equals("always");
		}
		return open;
	}

}
