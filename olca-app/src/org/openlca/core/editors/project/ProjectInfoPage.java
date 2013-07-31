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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.IContentChangedListener;
import org.openlca.app.Messages;
import org.openlca.app.component.IModelDropHandler;
import org.openlca.app.component.ObjectDialog;
import org.openlca.app.component.TextDropComponent;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.DateFormatter;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.app.util.Viewers;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form page for editing projects
 * 
 * @author Sebastian Greve
 * 
 */
public class ProjectInfoPage extends ModelEditorInfoPage implements
		PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * {@link TextDropComponent} for the author field
	 */
	private TextDropComponent authorDropComponent;

	/**
	 * A {@link Text} widget for the creation date-field of this project
	 */
	private Text creationDateText;

	/**
	 * A {@link Text} widget for the functional unit-field of this project
	 */
	private Text functionalUnitText;

	/**
	 * A {@link Text} widget for the goal-field of this project
	 */
	private Text goalText;

	/**
	 * A {@link Text} widget for the last modification date-field of this
	 * project
	 */
	private Text lastModificationDateText;

	/**
	 * Open editor action
	 */
	private OpenEditorAction openAction;

	/**
	 * Section for the productSystemsTableViewer
	 */
	private Section productSystemsInfoSection;

	private TableViewer systemViewer;

	/**
	 * the actor object edited by this editor
	 */
	private Project project = null;

	/**
	 * Creates a new instance.
	 * 
	 * @param editor
	 *            the editor of this page
	 */
	public ProjectInfoPage(final ModelEditor editor) {
		super(editor, "ProjectInfoPage", Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		this.project = (Project) editor.getModelComponent();
		this.project.addPropertyChangeListener(this);
	}

	/**
	 * Loads all product system descriptors of the project
	 * 
	 * @return All product system descriptors of the project
	 */
	private IModelComponent[] loadProductSystemDescriptors() {
		final List<IModelComponent> productSystemDescriptors = new ArrayList<>();
		for (final String id : project.getProductSystems()) {
			try {
				productSystemDescriptors.add(getDatabase().selectDescriptor(
						ProductSystem.class, id));
			} catch (final Exception e) {
				log.error("Reading product system from db failed", e);
			}
		}
		return productSystemDescriptors
				.toArray(new IModelComponent[productSystemDescriptors.size()]);
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		super.createContents(body, toolkit);
		final int heightHint = getManagedForm().getForm().computeSize(
				SWT.DEFAULT, SWT.DEFAULT).y / 3;

		final Section goalAndScopeInfoSection = UIFactory.createSection(body,
				toolkit, Messages.Projects_GoalAndScopeInfoSectionLabel, true,
				false);

		final Composite goalAndScopeInfoComposite = UIFactory
				.createSectionComposite(goalAndScopeInfoSection, toolkit,
						UIFactory.createGridLayout(2));

		goalText = UIFactory.createTextWithLabel(goalAndScopeInfoComposite,
				toolkit, Messages.Projects_Goal, true);

		functionalUnitText = UIFactory.createTextWithLabel(
				goalAndScopeInfoComposite, toolkit,
				Messages.Projects_FunctionalUnit, true);

		final Section projectInfoSection = UIFactory
				.createSection(body, toolkit,
						Messages.Projects_ProjectInfoSectionLabel, true, false);

		final Composite projectInfoComposite = UIFactory
				.createSectionComposite(projectInfoSection, toolkit,
						UIFactory.createGridLayout(2));

		creationDateText = UIFactory.createTextWithLabel(projectInfoComposite,
				toolkit, Messages.Projects_CreationDate, false);
		creationDateText.setEditable(false);

		lastModificationDateText = UIFactory.createTextWithLabel(
				projectInfoComposite, toolkit,
				Messages.Projects_LastModificationDate, false);
		lastModificationDateText.setEditable(false);

		authorDropComponent = createDropComponent(projectInfoComposite,
				toolkit, Messages.Projects_Author, project.getAuthor(),
				Actor.class, false);

		productSystemsInfoSection = UIFactory.createSection(body, toolkit,
				Messages.Projects_ProductSystemsInfoSectionLabel, true, true);

		final Composite productSystemsInfoComposite = UIFactory
				.createSectionComposite(productSystemsInfoSection, toolkit,
						UIFactory.createGridLayout(1, true, 0));

		systemViewer = UIFactory.createTableViewer(productSystemsInfoComposite,
				ProductSystem.class, new ProductSystemDropHandler(), toolkit,
				null, getDatabase());
		systemViewer.setLabelProvider(new ProductSystemsLabel());
		final GridData productSystemsGridData = new GridData(SWT.FILL,
				SWT.FILL, true, true);
		productSystemsGridData.heightHint = heightHint;
		systemViewer.getTable().setLayoutData(productSystemsGridData);

		bindActions(systemViewer, productSystemsInfoSection);

		openAction = new OpenEditorAction();
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = new AddProductSystemAction();
		Action remove = new RemoveProductSystemAction();
		UI.bindActions(section, add, remove);
		UI.bindActions(viewer, add, remove);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Projects_FormText
				+ ": "
				+ (project != null ? project.getName() != null ? project
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {
		super.initListeners();
		goalText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				project.setGoal(goalText.getText());
			}

		});

		functionalUnitText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				project.setFunctionalUnit(functionalUnitText.getText());
			}

		});

		productSystemsInfoSection
				.addExpansionListener(new IExpansionListener() {

					@Override
					public void expansionStateChanged(final ExpansionEvent e) {

					}

					@Override
					public void expansionStateChanging(final ExpansionEvent e) {
						((GridData) productSystemsInfoSection.getLayoutData()).grabExcessVerticalSpace = e
								.getState();
					}
				});

		systemViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.getFirstElement() != null) {
					final IModelComponent component = (IModelComponent) selection
							.getFirstElement();
					openAction.setModelComponent(getDatabase(), component);
					openAction.run();
				}
			}
		});

		authorDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(final Control source,
							final Object content) {
						if (content != null) {
							Actor author;
							try {
								author = getDatabase().select(Actor.class,
										((IModelComponent) content).getId());
								project.setAuthor(author);
							} catch (final Exception e) {
								log.error("Reading actor from db failed", e);
							}

						} else {
							project.setAuthor(null);
						}
					}

				});

	}

	@Override
	protected void setData() {
		super.setData();
		if (project.getGoal() != null) {
			goalText.setText(project.getGoal());
		}

		if (project.getFunctionalUnit() != null) {
			functionalUnitText.setText(project.getFunctionalUnit());
		}

		if (project.getCreationDate() != null) {
			creationDateText.setText(DateFormatter.formatShort(project
					.getCreationDate()));
			creationDateText.setToolTipText(DateFormatter.formatLong(project
					.getCreationDate()));
		}

		if (project.getLastModificationDate() != null) {
			lastModificationDateText.setText(DateFormatter.formatShort(project
					.getLastModificationDate()));
			lastModificationDateText.setToolTipText(DateFormatter
					.formatLong(project.getLastModificationDate()));
		}

		if (project.getProductSystems() != null) {
			systemViewer.setInput(loadProductSystemDescriptors());
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		project = null;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("lastModificationDate")) {
			if (lastModificationDateText != null) {
				lastModificationDateText.setText(DateFormatter
						.formatShort(project.getLastModificationDate()));
				lastModificationDateText.setToolTipText(DateFormatter
						.formatLong(project.getLastModificationDate()));
			}
		}
	}

	/**
	 * Adds a product system object to this project
	 * 
	 * @see Action
	 */
	private class AddProductSystemAction extends Action {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.editors.ProjectInfoPage.AddProductSystemAction";

		/**
		 * The text of the action
		 */
		public String TEXT = Messages.Projects_AddProductSystemText;

		/**
		 * Creates a new AddProductSystemAction and sets the ID, TEXT and
		 * ImageDescriptor
		 */
		public AddProductSystemAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());

		}

		@Override
		public void run() {
			NavigationRoot root = null;
			final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(Navigator.ID);
			if (navigator != null) {
				root = navigator.getRoot();
			}
			final ObjectDialog dialog = new ObjectDialog(
					UI.shell(), root, true, getDatabase(), ProductSystem.class);
			dialog.addFilter(new ViewerFilter() {

				@Override
				public boolean select(final Viewer viewer,
						final Object parentElement, final Object element) {
					boolean select = true;
					if (parentElement instanceof ModelElement) {
						// filter sub elements of product system navigation
						// element
						select = false;
					}
					return select;
				}
			});
			dialog.open();
			final int code = dialog.getReturnCode();
			if (code == Window.OK && dialog.getMultiSelection() != null) {
				final ProductSystem[] productSystems = new ProductSystem[dialog
						.getMultiSelection().length];
				for (int i = 0; i < dialog.getMultiSelection().length; i++) {
					if (dialog.getMultiSelection()[i] instanceof ProductSystem) {
						final ProductSystem productSystem = (ProductSystem) dialog
								.getMultiSelection()[i];
						project.addProductSystem(productSystem.getId());
						productSystems[i] = productSystem;
					}
				}
				systemViewer.setInput(loadProductSystemDescriptors());
				systemViewer.setSelection(new StructuredSelection(
						productSystems));
			}

		}
	}

	private class ProductSystemDropHandler implements IModelDropHandler {

		@Override
		public void handleDrop(final IModelComponent[] droppedComponents) {
			ProductSystem[] productSystems = new ProductSystem[droppedComponents.length];
			for (int i = 0; i < droppedComponents.length; i++) {
				if (droppedComponents[i] instanceof ProductSystem) {
					final ProductSystem productSystem = (ProductSystem) droppedComponents[i];
					project.addProductSystem(productSystem.getId());
					productSystems[i] = productSystem;
				}
			}
			systemViewer.setInput(loadProductSystemDescriptors());
			systemViewer.setSelection(new StructuredSelection(productSystems));
		}

	}

	private class RemoveProductSystemAction extends Action {

		public RemoveProductSystemAction() {
			setId("ProjectInfoPage.RemoveProductSystemAction");
			setText(Messages.Projects_RemoveProductSystemText);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		}

		@Override
		public void run() {
			List<ProductSystem> systems = Viewers.getAllSelected(systemViewer);
			for (ProductSystem system : systems)
				project.removeProductSystem(system.getId());
			systemViewer.setInput(loadProductSystemDescriptors());
		}
	}

}
