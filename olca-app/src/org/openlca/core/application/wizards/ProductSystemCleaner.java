/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Messages;
import org.openlca.app.components.delete.Problem;
import org.openlca.app.components.delete.ProblemWizard;
import org.openlca.app.util.UI;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Looks up the product system for potential problems for the calculation,
 * displays them and solves them if possible
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemCleaner {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Return value if the user canceled the operation
	 */
	public static int CANCELED = 2;

	/**
	 * Return value if the user decided to clean up the product system
	 */
	public static int CLEANED = 0;

	/**
	 * Return value if no problems were assessed
	 */
	public static int NO_PROBLEMS = 1;

	/**
	 * The product system to analyze
	 */
	private final ProductSystem productSystem;

	/**
	 * Creates a new instance
	 * 
	 * @param productSystem
	 *            The product system to analyze
	 */
	public ProductSystemCleaner(final ProductSystem productSystem) {
		this.productSystem = productSystem;
	}

	/**
	 * Getter of the assessed problems
	 * 
	 * @return The assessed problems
	 */
	private Problem[] getProblems() {
		final List<Problem> problems = new ArrayList<>();
		for (final Problem problem : getUnconnectedPartProblems()) {
			problems.add(problem);
		}
		return problems.toArray(new Problem[problems.size()]);
	}

	/**
	 * Creates a problem for each unconnected process (not connected to the
	 * product system's reference process or a supply process of the reference
	 * process)
	 * 
	 * @return The created problems
	 */
	private Problem[] getUnconnectedPartProblems() {
		final List<Problem> problems = new ArrayList<>();
		final List<Long> linkedToReference = new ArrayList<>();
		final Queue<Process> toCheck = new LinkedList<>();
		toCheck.add(productSystem.getReferenceProcess());

		// while more processes to check
		while (!toCheck.isEmpty()) {
			// get next
			final Process process = toCheck.poll();
			// if not linked to reference through any process
			if (!linkedToReference.contains(process.getId())) {
				// add as linked
				linkedToReference.add(process.getId());

				// for each incoming link of the process
				for (final ProcessLink processLink : productSystem
						.getIncomingLinks(process.getId())) {
					// add as to check
					toCheck.add(processLink.getProviderProcess());
				}
			}
		}

		// for each process of the product system
		for (final Process process : productSystem.getProcesses()) {
			// if not linked to reference
			if (!linkedToReference.contains(process.getId())) {
				// create a warning
				final Problem problem = new Problem(Problem.WARNING, NLS.bind(
						Messages.ProductSystemCleaner_ProblemText1,
						process.getName())) {

					@Override
					public void solve() {
						// remove links and processes which are not linked to
						// the reference process
						final ProcessLink[] links = productSystem
								.getProcessLinks(process.getId());
						for (final ProcessLink link : links) {
							productSystem.getProcessLinks().remove(link);
						}
						// TODO: remove processes?
						// productSystem.getProcesses().remove(process);
					}
				};
				problems.add(problem);
			}
		}
		return problems.toArray(new Problem[problems.size()]);
	}

	/**
	 * Cleans up the product system (Solves the assessed problems)
	 * 
	 * @return The user selected operation as int value
	 */
	public int cleanUp(final boolean showProgress) {
		int returnValue = -1;
		if (showProgress) {
			final List<Problem> problems = new ArrayList<>();
			try {
				PlatformUI.getWorkbench().getProgressService()
						.busyCursorWhile(new IRunnableWithProgress() {

							@Override
							public void run(final IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								monitor.beginTask(
										Messages.ProductSystemCleaner_Analizing,
										IProgressMonitor.UNKNOWN);
								// add each problem
								for (final Problem problem : getProblems()) {
									problems.add(problem);
								}
								monitor.done();
							}
						});
			} catch (final InvocationTargetException e) {
				log.error("Target invocation failed", e);
			} catch (final InterruptedException e) {
				log.error("Clean up interupted", e);
			}

			// create problem wizard dialog to show warnings
			final ProblemWizard wizard = new ProblemWizard(
					problems.toArray(new Problem[problems.size()]), false);
			final WizardDialog wizardDialog = new WizardDialog(UI.shell(),
					wizard) {

				@Override
				protected Control createContents(final Composite parent) {
					final Control control = super.createContents(parent);
					setTitle(Messages.ProductSystemCleaner_Title);
					return control;
				}

			};

			if (wizard.hasProblems()) {
				if (wizardDialog.open() == Window.OK && wizard.solvedProblems()) {
					returnValue = CLEANED;
				} else {
					returnValue = CANCELED;
				}
			} else {
				returnValue = NO_PROBLEMS;
			}
		} else {
			for (final Problem problem : getProblems()) {
				problem.solve();
			}
			returnValue = CLEANED;
		}

		return returnValue;
	}
}
