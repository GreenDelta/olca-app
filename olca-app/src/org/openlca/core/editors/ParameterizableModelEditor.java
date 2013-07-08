/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.core.application.Messages;
import org.openlca.core.model.IParameterisable;
import org.openlca.core.model.Parameter;
import org.openlca.ui.UI;

/**
 * Form editor for parameterizable components
 * 
 * @author sg
 * 
 */
public abstract class ParameterizableModelEditor extends ModelEditor {

	private final String componentTypeName;
	private boolean parameterErrors = false;
	private ModelParametersPage parametersPage;

	public ParameterizableModelEditor(String componentTypeName) {
		this.componentTypeName = componentTypeName;
	}

	@Override
	protected void addPages() {
		super.addPages();
		parametersPage = new ModelParametersPage(this, componentTypeName);
		try {
			addPage(parametersPage);
		} catch (final PartInitException e) {
			log.error("Add page failed", e);
		}
	}

	protected void initEvaluationController() {
		IParameterisable component = (IParameterisable) getModelComponent();
		for (Parameter parameter : component.getParameters()) {
		}
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		if (!parameterErrors) {
			super.doSave(monitor);
		} else {
			MessageDialog.openError(UI.shell(), Messages.CannotSave,
					Messages.ErrorText);
		}
	}

	@Override
	public void init(final IEditorSite site, final IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		initEvaluationController();
	}

	/**
	 * Reevaluates the expressions
	 */
	public void udpateExpressions() {
		initEvaluationController();
	}

}
