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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.JobListenerWithProgress;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.referencesearch.IReferenceSearcher;
import org.openlca.core.model.referencesearch.Reference;
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
public class DeleteWizard extends ProblemWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Creates a new delete wizard for the given object and problem handler
	 * 
	 * @param database
	 *            The database
	 * @param problemHandler
	 *            the problem handler belonging to the object
	 * @param object
	 *            the object to be deleted
	 */
	public DeleteWizard(final IDatabase database,
			final IReferenceSearcher<?> problemHandler, final Object object) {
		initializeProblems(database, problemHandler, object);
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.DeleteWizard_WindowTitle);
	}

	/**
	 * Initializes the problems
	 * 
	 * @param database
	 *            The database
	 * @param referenceSearcher
	 *            The reference searcher
	 * @param object
	 *            The object to search references for
	 */
	private void initializeProblems(
			final IDatabase database,
			@SuppressWarnings("rawtypes") final IReferenceSearcher referenceSearcher,
			final Object object) {
		if (referenceSearcher != null) {
			try {
				// initialize problems
				PlatformUI.getWorkbench().getProgressService()
						.busyCursorWhile(new JobListenerWithProgress() {

							@SuppressWarnings("unchecked")
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
								for (final Reference reference : referenceSearcher
										.findReferences(database, object)) {

									String text = "";

									if (reference.getReferencedFieldName() == null) {
										// if reference is necessary
										if (reference.getType() == Reference.REQUIRED) {
											// create text for necessary
											// reference
											text = NLS
													.bind(Messages.IsUsedAndCannotBeDeleted,
															new String[] {
																	Messages.Object,
																	reference
																			.getReferencedObjectType(),
																	reference
																			.getReferencedObjectName() });
										} else {
											// create text for unnecessary
											// reference
											text = NLS
													.bind(Messages.IsUsedAndWillBeRemoved,
															new String[] {
																	Messages.Object,
																	reference
																			.getReferencedObjectType(),
																	reference
																			.getReferencedObjectName() });
										}
									} else {
										// create text for necessary reference
										if (reference.getType() == Reference.REQUIRED) {
											text = NLS
													.bind(Messages.IsUsedAsAndCannotBeDeleted,
															new String[] {
																	Messages.Object,
																	reference
																			.getReferencedObjectType(),
																	reference
																			.getReferencedObjectName(),
																	reference
																			.getReferencedFieldName() });
										} else {
											// create text for unnecessary
											// reference
											text = NLS
													.bind(Messages.IsUsedAsAndWillBeRemoved,
															new String[] {
																	Messages.Object,
																	reference
																			.getReferencedObjectType(),
																	reference
																			.getReferencedObjectName(),
																	reference
																			.getReferencedFieldName() });
										}
									}

									// create problem
									final Problem problem = new Problem(
											reference.getType() == Reference.OPTIONAL ? Problem.WARNING
													: Problem.ERROR, text) {

										@Override
										public void solve() {
											// solve the problem
											reference.solve();
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
}
