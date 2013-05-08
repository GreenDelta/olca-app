/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.process;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.Process;
import org.openlca.core.model.Technology;
import org.openlca.core.model.Time;
import org.openlca.ui.BaseLabelProvider;
import org.openlca.ui.BaseNameSorter;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.viewer.LocationViewer;
import org.openlca.ui.viewer.ToolTipComboViewer;

/**
 * Form page for displaying and editing the general information of a process.
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessInfoPage extends ModelEditorInfoPage implements
		PropertyChangeListener {

	private Text geographyCommentText;
	private Button infrastructureProcessButton;
	private LocationViewer locationViewer;
	private Process process;
	private ToolTipComboViewer quantitativeReferenceCombo;
	private Technology technology;
	private Text technologyDescriptionText;

	private ProcessTimeSection timeSection;

	public ProcessInfoPage(ModelEditor editor, Technology technology, Time time) {
		super(editor, "ProcessInfoPage", Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		this.process = (Process) editor.getModelComponent();
		this.technology = technology;
		timeSection = new ProcessTimeSection(time);
		editor.getModelComponent().addPropertyChangeListener(this);
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		super.createContents(body, toolkit);

		infrastructureProcessButton = UIFactory.createButton(
				getMainComposite(), toolkit,
				Messages.Processes_InfrastructureProcess);

		final Section qRefSection = UIFactory.createSection(body, toolkit,
				Messages.Common_QuantitativeReference, true, false);

		final Composite qRefComposite = UIFactory.createSectionComposite(
				qRefSection, toolkit, UIFactory.createGridLayout(2));

		toolkit.createLabel(qRefComposite,
				Messages.Common_QuantitativeReference);
		quantitativeReferenceCombo = new ToolTipComboViewer(qRefComposite,
				SWT.NONE);
		quantitativeReferenceCombo.setLabelProvider(new BaseLabelProvider(
				getDatabase()));
		quantitativeReferenceCombo.setContentProvider(ArrayContentProvider
				.getInstance());
		quantitativeReferenceCombo.setSorter(new BaseNameSorter());
		quantitativeReferenceCombo.setLayoutData(new GridData(SWT.FILL,
				SWT.FILL, true, false));
		quantitativeReferenceCombo.setInput(process
				.getOutputs(FlowType.ProductFlow));

		timeSection.createSection(toolkit, body);

		final Section locationInfoSection = UIFactory.createSection(body,
				toolkit, Messages.Processes_LocationInfoSectionLabel, true,
				false);

		final Composite locationInfoComposite = UIFactory
				.createSectionComposite(locationInfoSection, toolkit,
						UIFactory.createGridLayout(2));

		UI.formLabel(locationInfoComposite, Messages.Location);
		locationViewer = new LocationViewer(locationInfoComposite);

		geographyCommentText = UIFactory.createTextWithLabel(
				locationInfoComposite, toolkit,
				Messages.Processes_GeographyComment, true);

		final Section technologyInfoSection = UIFactory.createSection(body,
				toolkit, Messages.Processes_TechnologyInfoSectionLabel, true,
				false);

		final Composite technologyInfoComposite = UIFactory
				.createSectionComposite(technologyInfoSection, toolkit,
						UIFactory.createGridLayout(2));

		technologyDescriptionText = UIFactory.createTextWithLabel(
				technologyInfoComposite, toolkit, Messages.Common_Description,
				true);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Processes_FormText
				+ ": "
				+ (process != null ? process.getName() != null ? process
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {
		super.initListeners();
		infrastructureProcessButton
				.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetDefaultSelected(final SelectionEvent e) {
						// no action on default selection
					}

					@Override
					public void widgetSelected(final SelectionEvent e) {
						process.setInfrastructureProcess(infrastructureProcessButton
								.getSelection());
					}
				});

		quantitativeReferenceCombo
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();
						if (!selection.isEmpty()) {
							process.setQuantitativeReference((Exchange) selection
									.getFirstElement());
						}
					}
				});

		locationViewer
				.addSelectionChangedListener(new org.openlca.ui.viewer.ISelectionChangedListener<Location>() {

					@Override
					public void selectionChanged(Location selection) {
						process.setLocation(selection);
					};

				});

		geographyCommentText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				process.setGeographyComment(geographyCommentText.getText());
			}

		});

		technologyDescriptionText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				technology.setDescription(technologyDescriptionText.getText());
			}

		});

	}

	@Override
	protected void setData() {
		super.setData();
		locationViewer.setInput(getDatabase());
		if (process != null) {
			infrastructureProcessButton.setSelection(process
					.isInfrastructureProcess());
			if (process.getGeographyComment() != null) {
				geographyCommentText.setText(process.getGeographyComment());
			}
			if (technology != null) {
				if (technology.getDescription() != null) {
					technologyDescriptionText.setText(technology
							.getDescription());
				}
			}
			if (process.getQuantitativeReference() != null) {
				quantitativeReferenceCombo
						.setSelection(new StructuredSelection(process
								.getQuantitativeReference()));
			}
			locationViewer.select(process.getLocation());
		}
	}

	@Override
	public void dispose() {
		if (getEditor() != null
				&& ((ModelEditor) getEditor()).getModelComponent() != null) {
			((ModelEditor) getEditor()).getModelComponent()
					.removePropertyChangeListener(this);
		}
		super.dispose();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

		if (evt.getPropertyName().equals("exchanges")) {
			// update the quantitative reference combo
			if (quantitativeReferenceCombo != null) {
				quantitativeReferenceCombo.setInput(process
						.getOutputs(FlowType.ProductFlow));
				if (process.getQuantitativeReference() != null) {
					quantitativeReferenceCombo
							.setSelection(new StructuredSelection(process
									.getQuantitativeReference()));
				}
			}
		}

	}
}
