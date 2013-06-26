/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.model.RootEntity;
import org.openlca.ui.UIFactory;

/**
 * Abstract form page for model component information (name, description,
 * category)
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ModelEditorInfoPage extends ModelEditorPage implements
		ISelectionProvider {

	/**
	 * A {@link Text} widget for the description-field of this actor
	 */
	private Text descriptionText;

	/**
	 * Selection changed listeners
	 */
	private final List<ISelectionChangedListener> listeners = new ArrayList<>();

	/**
	 * The composite of the name, description and category
	 */
	private Composite mainComposite;

	/**
	 * The title of the main section
	 */
	private String mainSectionTitle;

	/**
	 * the object edited by this editor
	 */
	private RootEntity modelComponent;

	/**
	 * A {@link Text} widget for the name-field of this actor
	 */
	private Text nameText;

	/**
	 * Actual selection
	 */
	private IStructuredSelection selection = new StructuredSelection();

	/**
	 * Creates a new model editor info page
	 * 
	 * @param editor
	 *            The form editor
	 * @param id
	 *            The id of the page
	 * @param title
	 *            The title of the page
	 * @param mainSectionTitle
	 *            The title of the main section
	 */
	public ModelEditorInfoPage(final ModelEditor editor, final String id,
			final String title, final String mainSectionTitle) {
		super(editor, id, title);
		this.mainSectionTitle = mainSectionTitle;
		this.modelComponent = editor.getModelComponent();
	}

	/**
	 * Adds actions to the given section
	 * 
	 * @param section
	 *            The sections to add actions on
	 */
	protected void addSectionActions(final Section section) {
		// subclasses may override
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		// create section
		final Section section = UIFactory.createSection(body, toolkit,
				mainSectionTitle, true, false);
		addSectionActions(section);

		// create body
		mainComposite = UIFactory.createSectionComposite(section, toolkit,
				UIFactory.createGridLayout(2));

		nameText = UIFactory.createTextWithLabel(mainComposite, toolkit,
				Messages.Common_Name, false);

		descriptionText = UIFactory.createTextWithLabel(mainComposite, toolkit,
				Messages.Common_Description, true);

	}

	@Override
	protected void initListeners() {
		nameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				modelComponent.setName(nameText.getText());
			}

		});

		descriptionText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				modelComponent.setDescription(descriptionText.getText());
			}

		});

	}

	@Override
	protected void setData() {
		nameText.setText(modelComponent.getName());
		if (modelComponent.getDescription() != null) {
			descriptionText.setText(modelComponent.getDescription());
		}
	}

	@Override
	public void addSelectionChangedListener(
			final ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void dispose() {
		super.dispose();
		modelComponent = null;
		nameText = null;
		descriptionText = null;
		mainSectionTitle = null;
	}

	/**
	 * Getter of the mainComposite-field
	 * 
	 * @return The composite of the name, description and category
	 */
	public final Composite getMainComposite() {
		return mainComposite;
	}

	@Override
	public ISelection getSelection() {
		return selection;
	}

	@Override
	public void removeSelectionChangedListener(
			final ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSelection(final ISelection selection) {
		this.selection = (IStructuredSelection) selection;
		for (final ISelectionChangedListener listener : listeners) {
			listener.selectionChanged(new SelectionChangedEvent(this, selection));
		}
	}

}
