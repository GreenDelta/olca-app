/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.actions;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.JobListenerWithProgress;
import org.openlca.app.db.Database;
import org.openlca.app.editors.AnalyzeEditorInput;
import org.openlca.app.editors.CalculationType;
import org.openlca.app.editors.actions.CalculationWizardPage.CalculationSettings;
import org.openlca.app.util.Editors;
import org.openlca.core.editors.analyze.AnalyzeEditor;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for setting calculation properties and run the calculation of a
 * product system
 */
class CalculationWizard extends Wizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private CalculationWizardPage calculationPage;
	private ProductSystem productSystem;

	public CalculationWizard(ProductSystem productSystem) {
		this.productSystem = productSystem;
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.CalculationWizardTitle);
	}

	@Override
	public void addPages() {
		calculationPage = new CalculationWizardPage();
		addPage(calculationPage);
	}

	@Override
	public boolean performFinish() {
		CalculationSettings settings = calculationPage.getSettings();
		switch (settings.getType()) {
		case ANALYSIS:
		case QUICK:
			analyze(settings);
			return true;
		case MONTE_CARLO:
			// TODO
			// SimulationInit init = new SimulationInit(productSystem, database,
			// settings.getIterationCount());
			// init.setImpactMethod(settings.getMethod());
			// init.setAllocationMethod(settings.getAllocationMethod());
			// init.run();
			return true;
		default:
			return false;
		}
	}

	private void analyze(final CalculationSettings settings) {
		try {
			getContainer().run(true, true, new JobListenerWithProgress() {

				@Override
				public void run() {
					JobHandler handler = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);
					handler.addJobListener(this);
					handler.startJob(Messages.Calculating,
							IProgressMonitor.UNKNOWN);
					SystemCalculator calculator = new SystemCalculator(Database
							.get());
					ImpactMethodDescriptor method = settings.getMethod();
					Object result = null;
					switch (settings.getType()) {
					case ANALYSIS:
						if (method != null)
							result = calculator.analyse(productSystem);
						else
							result = calculator.analyse(productSystem, method);
						break;
					case QUICK:
						if (method != null)
							result = calculator.solve(productSystem);
						else
							result = calculator.solve(productSystem, method);
						break;
					default:
						break;
					}
					if (result != null)
						openEditor(result, method, settings.getNwSet(),
								settings.getType());
					handler.done();
					handler.removeJobListener(this);
				}
			});
		} catch (Exception e) {
			log.error("Target invocation failed", e);
		}
	}

	private void openEditor(Object result, ImpactMethodDescriptor method,
			NormalizationWeightingSet nwSet, CalculationType type) {
		AnalyzeEditorInput input = new AnalyzeEditorInput();
		input.setType(type);
		if (method != null)
			input.setMethodId(method.getId());
		if (nwSet != null)
			input.setNwSetId(nwSet.getId());
		String resultKey = UUID.randomUUID().toString();
		App.getCache().put(resultKey, result);
		input.setResultKey(resultKey);
		Editors.open(input, AnalyzeEditor.ID);
	}

}
