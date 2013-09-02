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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.AnalyzeEditorInput;
import org.openlca.app.util.Editors;
import org.openlca.core.editors.analyze.AnalyzeEditor;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.AnalysisResult;
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
		calculationPage = new CalculationWizardPage(productSystem);
		addPage(calculationPage);
	}

	@Override
	public boolean performFinish() {
		CalculationSetup settings = calculationPage.getSetup();
		try {
			getContainer().run(true, true, new Calculation(settings));
			return true;
		} catch (Exception e) {
			log.error("Calculation failed", e);
			return false;
		}
	}

	private class Calculation implements IRunnableWithProgress {

		private CalculationSetup settings;

		public Calculation(CalculationSetup settings) {
			this.settings = settings;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask("Run calculation", IProgressMonitor.UNKNOWN);
			if (settings.hasType(CalculationSetup.QUICK_RESULT))
				solve();
			else if (settings.hasType(CalculationSetup.ANALYSIS))
				analyse();
			// TODO: Monte-Carlo-Simulation
			monitor.done();
		}

		private void analyse() {
			log.trace("run analysis");
			SystemCalculator calculator = new SystemCalculator(Database.get());
			AnalysisResult analysisResult = calculator.analyse(settings);
			log.trace("calculation done, open editor");
			String resultKey = App.getCache().put(analysisResult);
			String setupKey = App.getCache().put(settings);
			AnalyzeEditorInput input = new AnalyzeEditorInput(setupKey,
					resultKey);
			Editors.open(input, AnalyzeEditor.ID);
		}

		private void solve() {
			log.trace("run quick calculation");
			// SystemCalculator calculator = new
			// SystemCalculator(Database.get());
			// InventoryResult inventoryResult = calculator.solve(settings);
			log.trace("calculation done, open editor");
			// openEditor(result);
		}

		// private void openEditor(Calc result) {
		// AnalyzeEditorInput input = new AnalyzeEditorInput();
		// String resultKey = UUID.randomUUID().toString();
		// App.getCache().put(resultKey, result);
		// input.setResultKey(resultKey);
		// Editors.open(input, AnalyzeEditor.ID);
		// }
	}
}
