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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UIFactory;

/**
 * Wizard page for displaying warnings and errors while deleting an object
 * 
 */
public class ProblemsPage extends WizardPage {

	/**
	 * The id of this page
	 */
	public static final String ID = "org.openlca.core.application.deletewizard.ProblemsPage";

	/**
	 * The problems occured while deleting
	 */
	private final Problem[] problems;

	/**
	 * Creates a new page with the given problems
	 * 
	 * @param problems
	 *            the problems to be displayed
	 */
	protected ProblemsPage(final Problem[] problems) {
		super(ID);
		this.problems = problems;
		setTitle(Messages.Delete);
		setDescription(Messages.FoundProblems + " "
				+ problems.length);
		setPageComplete(!hasErrors());
	}

	/**
	 * Checks if errors are in the list of problems
	 * 
	 * @return true, if errors exists, false if only warnings exist
	 */
	private boolean hasErrors() {
		boolean hasErrors = false;
		int i = 0;
		while (!hasErrors && i < problems.length) {
			if (problems[i].getType() == Problem.ERROR) {
				hasErrors = true;
			} else {
				i++;
			}
		}
		return hasErrors;
	}

	@Override
	public void createControl(final Composite parent) {
		// create body
		final Composite body = UIFactory.createContainer(parent,
				UIFactory.createGridLayout(1, true, 0));

		// create table viewer for displaying problems
		final TableViewer problemsViewer = new TableViewer(body);
		problemsViewer.setContentProvider(new ProblemContentProvider());
		problemsViewer.setLabelProvider(new ProblemLabelProvider());
		problemsViewer.setSorter(new ProblemSorter());
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		problemsViewer.getTable().setLayoutData(gd);
		problemsViewer.setInput(problems);
		setControl(body);
	}

	/**
	 * Content provider for the problem viewer
	 */
	private class ProblemContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {

		}

		@Override
		public Object[] getElements(final Object inputElement) {
			Object[] objects = new Object[0];
			if (inputElement instanceof Problem[]) {
				objects = (Problem[]) inputElement;
			}
			return objects;
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {

		}

	}

	/**
	 * Label provider for the problem viewer
	 */
	private class ProblemLabelProvider extends LabelProvider {

		@Override
		public Image getImage(final Object element) {
			Image image = null;
			if (element instanceof Problem) {
				if (((Problem) element).getType() == Problem.ERROR) {
					image = ImageType.ERROR_ICON.get();
				} else if (((Problem) element).getType() == Problem.WARNING) {
					image = ImageType.WARNING_ICON.get();
				}
			}
			return image;
		}

		@Override
		public String getText(final Object element) {
			String text = null;
			if (element instanceof Problem) {
				text = ((Problem) element).getText();
			}
			return text;
		}

	}

	/**
	 * Sorter of the content of the problem viewer
	 */
	private class ProblemSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			int compare = 0;
			if (e1 instanceof Problem && e2 instanceof Problem) {
				compare = ((Problem) e1).getType().compareTo(
						((Problem) e2).getType());
			}
			return compare;
		}

	}

}
