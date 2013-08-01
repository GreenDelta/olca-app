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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Messages;
import org.openlca.app.components.JobListenerWithProgress;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for displaying warnings and errors while deleting an object
 * 
 * @see ProblemWizard
 * 
 * @author Sebastian Greve
 * 
 */
public class DeleteWizard<T extends RootEntity> extends ProblemWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Creates a new delete wizard for the given object and problem handler
	 * 
	 */
	public DeleteWizard(IUseSearch<T> search, T model) {
		initializeProblems(search, model);
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.DeleteWizard_WindowTitle);
	}

	/**
	 * Initializes the problems
	 */
	private void initializeProblems(final IUseSearch<T> search, final T model) {
		if (search != null) {
			try {
				// initialize problems
				PlatformUI.getWorkbench().getProgressService()
						.busyCursorWhile(new JobListenerWithProgress() {

							@Override
							public void run() {
								// initialize job handler
								final JobHandler handler = Jobs
										.getHandler(Jobs.MAIN_JOB_HANDLER);
								handler.addJobListener(this);
								handler.startJob(
										Messages.DeleteWizard_Analyzing,
										IProgressMonitor.UNKNOWN);
								final List<Problem> problems = new ArrayList<>();
								// for each found reference
								for (final BaseDescriptor descriptor : search
										.findUses(model)) {
									// create problem
									final Problem problem = new Problem(
											Problem.ERROR, getMessage(model,
													descriptor)) {

										@Override
										public void solve() {

										}
									};
									problems.add(problem);
								}
								setProblems(problems
										.toArray(new Problem[problems.size()]));
								handler.done();
								handler.removeJobListener(this);
							}
						});
			} catch (final Exception e) {
				log.error("Initialize problems failed", e);
			}
		}
	}

	private String getMessage(T used, BaseDescriptor user) {
		String s1 = used.getClass().getSimpleName() + " " + used.getName();
		String s2 = user.getModelType().getModelClass().getSimpleName() + " "
				+ user.getName();
		return s1 + " is used by " + s2;

	}

}
