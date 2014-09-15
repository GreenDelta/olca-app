/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.components.delete;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.openlca.app.Messages;

/**
 * Wizard for displaying problems
 */
public class ProblemWizard extends Wizard {

	/**
	 * The problems that occured
	 */
	private Problem[] problems;

	/**
	 * Indicates if the problems were solved
	 */
	private boolean solvedProblems = false;

	/**
	 * Creates a new ProblemWizard
	 */
	protected ProblemWizard() {
		problems = new Problem[0];
	}

	/**
	 * Creates a new ProblemWizard with the given problems
	 * 
	 * @param problems
	 *            the problems to be displayed
	 * @param useProgressMonitor
	 *            Indicates if a progress monitor should be used
	 */
	public ProblemWizard(final Problem[] problems,
			final boolean useProgressMonitor) {
		setProblems(problems);
		setNeedsProgressMonitor(useProgressMonitor);
	}

	/**
	 * Set the problems that should be displayed
	 * 
	 * @param problems
	 *            the problems that should be displayed
	 */
	protected void setProblems(final Problem[] problems) {
		if (this.problems != null) {
			final Problem[] oldProblems = this.problems;
			this.problems = new Problem[this.problems.length + problems.length];

			for (int i = 0; i < oldProblems.length; i++) {
				this.problems[i] = oldProblems[i];
			}

			for (int i = oldProblems.length; i < problems.length
					+ oldProblems.length; i++) {
				this.problems[i] = problems[i - oldProblems.length];
			}
		} else {
			this.problems = problems;
		}
	}

	@Override
	public void addPages() {
		addPage(new ProblemsPage(problems));
	}

	/**
	 * Getter of the problems
	 * 
	 * @return The problems that occured
	 */
	public Problem[] getProblems() {
		return problems;
	}

	/**
	 * Checks if there are any problems
	 * 
	 * @return true if the number of problems are larger than 0, false otherwise
	 */
	public boolean hasProblems() {
		return problems.length > 0;
	}

	@Override
	public boolean performFinish() {
		boolean errorOccured = false;
		if (needsProgressMonitor()) {
			// solve problems
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {

					@Override
					public void run(final IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.SolvingProblems,
								problems.length);
						// for each problem
						for (final Problem problem : problems) {
							// solve
							problem.solve();
							monitor.worked(1);
						}
						monitor.done();
					}
				});
			} catch (final Exception e) {
				errorOccured = true;
			}
		} else {
			for (final Problem problem : problems) {
				problem.solve();
			}
		}
		solvedProblems = !errorOccured;
		return solvedProblems;
	}

	/**
	 * Returns the state of the solved problems boolean
	 * 
	 * @return True if all problems were solved, false otherwise
	 */
	public boolean solvedProblems() {
		return solvedProblems;
	}

}
