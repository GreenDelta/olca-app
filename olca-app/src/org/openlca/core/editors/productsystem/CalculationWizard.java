/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.openlca.app.Editors;
import org.openlca.app.JobListenerWithProgress;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.views.AnalyzeEditorInput;
import org.openlca.core.application.views.ResultEditorInput;
import org.openlca.core.application.wizards.ProductSystemCleaner;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.analyze.AnalyzeEditor;
import org.openlca.core.editors.result.ResultEditor;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.math.ImpactCalculator;
import org.openlca.core.math.MatrixSolver;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.model.results.ImpactResult;
import org.openlca.core.model.results.InventoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for setting calculation properties and run the calculation of a
 * product system
 * 
 * @author Sebastian Greve
 * 
 */
class CalculationWizard extends Wizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private CalculationWizardPage calculationPage;
	private IDatabase database;
	private ProductSystem productSystem;

	public CalculationWizard(ProductSystem productSystem, IDatabase database) {
		this.productSystem = productSystem;
		this.database = database;
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.CalculationWizardTitle);
	}

	@Override
	public void addPages() {
		calculationPage = new CalculationWizardPage(database);
		addPage(calculationPage);
	}

	@Override
	public boolean performFinish() {
		CalculationSettings settings = calculationPage.getSettings();
		switch (settings.getType()) {
		case ANALYSIS:
			analyze(settings);
			return true;
		case MONTE_CARLO:
			SimulationInit init = new SimulationInit(productSystem, database,
					settings.getIterationCount());
			init.setImpactMethod(settings.getMethod());
			init.setAllocationMethod(settings.getAllocationMethod());
			init.run();
			return true;
		case QUICK:
			calculate(settings);
			return true;
		default:
			return false;
		}
	}

	private void analyze(CalculationSettings settings) {
		final MatrixSolver calculator = new MatrixSolver(database);
		calculator.setAllocationMethod(settings.getAllocationMethod());
		final ImpactMethodDescriptor method = settings.getMethod();
		calculator.setMethodDescriptor(method);
		final NormalizationWeightingSet nwSet = settings.getNwSet();

		try {
			getContainer().run(true, true, new JobListenerWithProgress() {

				@Override
				public void run() {
					JobHandler handler = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);
					handler.addJobListener(this);
					handler.startJob(Messages.LoadingPS,
							IProgressMonitor.UNKNOWN);

					ProductSystemCleaner cleaner = new ProductSystemCleaner(
							productSystem);
					cleaner.cleanUp(false);
					if (!handler.jobIsCanceled()) {
						handler.startJob(Messages.Calculating,
								IProgressMonitor.UNKNOWN);
						CalculateAction calculateAction = new CalculateAction();
						try {
							AnalysisResult result = calculateAction
									.calculateAggregatedProcessResults(
											productSystem, database, calculator);
							openAnalysisEditot(result, method, nwSet);

						} catch (Exception e) {
							log.error("Calculation failed " + e.getMessage(), e);
						}
					}
					handler.done();
					handler.removeJobListener(this);
				}
			});
		} catch (Exception e) {
			log.error("Target invocation failed", e);
		}
	}

	private void openAnalysisEditot(AnalysisResult result,
			ImpactMethodDescriptor method, NormalizationWeightingSet nwSet) {
		AnalyzeEditorInput input = new AnalyzeEditorInput();
		input.setDatabase(database);
		input.setMethodDescriptor(method);
		input.setNwSet(nwSet);
		String resultKey = UUID.randomUUID().toString();
		App.getCache().put(resultKey, result);
		input.setResultKey(resultKey);
		Editors.open(input, AnalyzeEditor.ID);
	}

	/**
	 * Calculates the LCI/LCIA result of the product system
	 */
	private void calculate(CalculationSettings settings) {
		final MatrixSolver calculator = new MatrixSolver(database);
		calculator.setAllocationMethod(settings.getAllocationMethod());
		final ImpactMethodDescriptor method = settings.getMethod();
		final NormalizationWeightingSet nwSet = getNwSet(settings.getNwSet());

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) {

					monitor.beginTask(Messages.CalculatingLCI,
							IProgressMonitor.UNKNOWN);

					try {
						CalculateAction calculateAction = new CalculateAction();
						InventoryResult lciResult = calculateAction.calculate(
								productSystem, database, calculator);
						cacheNewResult(lciResult);
						ResultEditorInput input = new ResultEditorInput(
								lciResult, database);

						monitor.subTask(Messages.CalculatingLCIA);
						if (method != null) {
							ImpactCalculator impactCalculator = new ImpactCalculator(
									database, lciResult);
							ImpactResult lciaResult = impactCalculator
									.calculate(method, nwSet);
							input.setImpactResult(lciaResult);
						}
						Editors.open(input, ResultEditor.ID);
					} catch (Exception e1) {
						log.error("Calculate LCI result failed", e1);
					}
				}

			});
		} catch (Exception e) {
			log.error("Target invocation failed", e);
		}

	}

	private NormalizationWeightingSet getNwSet(NormalizationWeightingSet nwSet) {
		if (nwSet == null)
			return null;
		try {
			BaseDao<NormalizationWeightingSet> dao = database
					.createDao(NormalizationWeightingSet.class);
			return dao.getForId(nwSet.getId());
		} catch (Exception e) {
			log.error("Failed to load nw-set " + nwSet, e);
			return null;
		}
	}

	private void cacheNewResult(InventoryResult newResult) {
		if (newResult == null)
			return;
		log.trace("cache new LCI result");
		try {
			BaseDao<InventoryResult> dao = Database
					.createDao(InventoryResult.class);
			InventoryResult oldResult = dao.getForId(newResult
					.getProductSystemId());
			if (oldResult != null)
				dao.delete(oldResult);
			dao.insert(newResult);
		} catch (Exception e) {
			log.error("Caching new LCI result failed", e);
		}
	}

}
