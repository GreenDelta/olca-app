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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.CategoryNavigationElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ui.UIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract form page for model component information (name, description,
 * category)
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ModelEditorInfoPage extends ModelEditorPage implements
		ISelectionProvider {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The section for the category tree viewer
	 */
	private Section categorySection;
	/**
	 * The category tree viewer
	 */
	private TreeViewer categoryTreeViewer;

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
	private IModelComponent modelComponent;

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
	 * Sets the input and initial selection of the category tree viewer
	 */
	private void setCategoryViewerData() {
		// set the category viewer input
		try {
			NavigationRoot root = null;
			final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(Navigator.ID);
			if (navigator != null) {
				root = navigator.getRoot();
			}
			if (root != null) {
				final CategoryNavigationElement catRoot = root.getCategoryRoot(
						modelComponent.getClass(), getDatabase());
				CategoryNavigationElement categoryElement = null;
				final Queue<CategoryNavigationElement> queue = new LinkedList<>();
				queue.add(catRoot);
				while (categoryElement == null && !queue.isEmpty()) {
					final CategoryNavigationElement next = queue.poll();
					if (((Category) next.getData()).getId().equals(
							modelComponent.getCategoryId())) {
						categoryElement = next;
					} else {
						for (final INavigationElement element : next
								.getChildren(false)) {
							if (element instanceof CategoryNavigationElement) {
								queue.add((CategoryNavigationElement) element);
							}
						}
					}
				}
				if (categoryElement != null) {
					categoryTreeViewer.setSelection(new StructuredSelection(
							categoryElement));
					final Category c = (Category) categoryElement.getData();
					categorySection.setText(c.getName());
				}
				categorySection.layout();
			}
		} catch (final Exception e) {
			log.error("Setting category viewer input failed", e);
		}
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

	/**
	 * Creates a {@link TreeViewer} widget for selecting the category of the
	 * model component
	 * 
	 * @param composite
	 *            the parent composite
	 * @param toolkit
	 *            the {@link FormToolkit} which should be used The model
	 *            component
	 * @return a {@link TreeViewer} widget
	 */
	protected final TreeViewer createCategoryTreeViewer(
			final Composite composite, final FormToolkit toolkit) {
		categorySection = UIFactory.createCategorySection(composite, toolkit);
		NavigationRoot root = null;
		final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(Navigator.ID);
		if (navigator != null) {
			root = navigator.getRoot();
		}
		// create category tree viewer
		final TreeViewer treeViewer = UIFactory.createCategoryTreeViewer(
				categorySection,
				toolkit,
				root != null ? root.getCategoryRoot(modelComponent.getClass(),
						getDatabase()).getParent() : null,
				modelComponent.getClass());
		return treeViewer;
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

		categoryTreeViewer = createCategoryTreeViewer(mainComposite, toolkit);
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

		categoryTreeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						if (event.getSelection() != null
								&& !event.getSelection().isEmpty()
								&& event.getSelection() instanceof IStructuredSelection) {
							final IStructuredSelection selection = (IStructuredSelection) event
									.getSelection();
							if (selection.getFirstElement() instanceof CategoryNavigationElement) {
								final Category c = (Category) ((CategoryNavigationElement) selection
										.getFirstElement()).getData();
								categorySection.setText(c.getName());
								categorySection.layout();
								modelComponent.setCategoryId(c.getId());
							}
						}

					}

				});
	}

	@Override
	protected void setData() {
		nameText.setText(modelComponent.getName());
		if (modelComponent.getDescription() != null) {
			descriptionText.setText(modelComponent.getDescription());
		}
		setCategoryViewerData();
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
