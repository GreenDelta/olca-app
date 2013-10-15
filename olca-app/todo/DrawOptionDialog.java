/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.BaseNameSorter;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.NormalizationWeightingSet;

/**
 * Dialog for selection the draw options for the chart
 * 
 * @author Sebastian Greve
 * 
 */
public class DrawOptionDialog extends FormDialog {

	private List<Button> buttons = new ArrayList<>();
	private HashMap<String, Boolean> drawCategories = new HashMap<>();
	private LCIAMethod method;
	private NormalizationWeightingSet normalizationWeightingSet;
	private ComboViewer normalizationWeightingSetViewer;
	private Button selectAllButton;
	private Button showValuesButton;
	private boolean showValuesOnBarSeries = false;
	private Button unselectAllButton;

	public DrawOptionDialog(LCIAMethod method) {
		super(UI.shell());
		this.method = method;
	}

	/**
	 * Initializes the listeners
	 */
	private void initListeners() {
		for (final Button button : buttons) {
			button.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(SelectionEvent e) {
					drawCategories.put(
							((LCIACategory) button.getData()).getId(),
							button.getSelection());
				}
			});
		}

		selectAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Button button : buttons) {
					button.setSelection(true);
				}
				for (LCIACategory category : method.getLCIACategories()) {
					drawCategories.put(category.getId(), true);
				}
			}
		});

		unselectAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Button button : buttons) {
					button.setSelection(false);
				}
				for (LCIACategory category : method.getLCIACategories()) {
					drawCategories.put(category.getId(), false);
				}
			}
		});
		showValuesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				showValuesOnBarSeries = showValuesButton.getSelection();
			}
		});

		normalizationWeightingSetViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						if (!normalizationWeightingSetViewer.getSelection()
								.isEmpty()) {
							normalizationWeightingSet = (NormalizationWeightingSet) ((IStructuredSelection) normalizationWeightingSetViewer
									.getSelection()).getFirstElement();
						}
					}
				});
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		toolkit.decorateFormHeading(form.getForm());
		String title = Messages.SelectCategories;
		form.setText(title);

		Composite body = UI.formBody(form, toolkit);
		body.setLayout(new GridLayout(1, true));

		Composite composite = toolkit.createComposite(body);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, true));

		List<LCIACategory> categories = new ArrayList<>();
		for (LCIACategory category : method.getLCIACategories()) {
			categories.add(category);
		}
		Collections.sort(categories, new Comparator<LCIACategory>() {

			@Override
			public int compare(LCIACategory arg0, LCIACategory arg1) {
				return arg0.getName().toLowerCase()
						.compareTo(arg1.getName().toLowerCase());
			}

		});

		for (LCIACategory category : categories) {
			Button button = toolkit.createButton(composite, category.getName(),
					SWT.CHECK);
			button.setData(category);
			buttons.add(button);
		}
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		if (categories.size() % 2 == 1) {
			new Label(composite, SWT.NONE);
		}

		Composite nwComposite = toolkit.createComposite(body);
		nwComposite
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		nwComposite.setLayout(new GridLayout(2, false));
		normalizationWeightingSetViewer = UIFactory.createComboViewerWithLabel(
				nwComposite, toolkit,
				Messages.NormalizationWeightingSet,
				ArrayContentProvider.getInstance(), new BaseLabelProvider(),
				new BaseNameSorter());
		normalizationWeightingSetViewer.setInput(method
				.getNormalizationWeightingSets());

		Composite composite2 = toolkit.createComposite(body);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		composite2.setLayout(new GridLayout(2, false));

		selectAllButton = toolkit.createButton(composite2,
				Messages.SelectAll, SWT.NONE);

		unselectAllButton = toolkit.createButton(composite2,
				Messages.SelectNone, SWT.NONE);

		Label sep = new Label(body, SWT.HORIZONTAL | SWT.SEPARATOR);
		sep.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		showValuesButton = toolkit.createButton(body,
				Messages.ShowValues, SWT.CHECK);

		initListeners();
	}

	/**
	 * Getter of the selected normalization and weighting set
	 * 
	 * @return The selected normalization and weighting set
	 */
	public NormalizationWeightingSet getNormalizationWeightingSet() {
		return normalizationWeightingSet;
	}

	/**
	 * Getter of the selected categories
	 * 
	 * @return an array with the selected lcia categories
	 */
	public LCIACategory[] getSelectedCategories() {
		List<LCIACategory> categories = new ArrayList<>();
		for (LCIACategory category : method.getLCIACategories()) {
			if (drawCategories.get(category.getId()) != null
					&& drawCategories.get(category.getId())) {
				categories.add(category);
			}
		}
		return categories.toArray(new LCIACategory[categories.size()]);
	}

	/**
	 * Getter of the showValuesOnBarSeries-field
	 * 
	 * @return true if the values of results should be shown on the bar series,
	 *         false otherwise
	 */
	public boolean showValuesOnBarSeries() {
		return showValuesOnBarSeries;
	}
}
