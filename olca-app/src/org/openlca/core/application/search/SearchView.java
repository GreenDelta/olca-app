/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ModelTypeViewer;
import org.openlca.app.viewers.table.DescriptorViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ilcd.methods.LCIAMethod;
import org.openlca.ilcd.processes.LCIAResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchView extends ViewPart {

	public static final String ID = "views.search";

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ModelTypeViewer modelTypeViewer;
	private DescriptorViewer resultViewer;
	private Text searchText;
	private Button searchButton;

	public static void refresh() {
		SearchView searchView = (SearchView) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(SearchView.ID);
		if (searchView != null)
			searchView.doRefresh();
	}

	@Override
	public void setFocus() {
		if (searchText != null)
			searchText.setFocus();
	}

	private void initListeners() {
		modelTypeViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ModelType>() {

					@Override
					public void selectionChanged(ModelType selection) {
						search();
					}

				});

		resultViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				BaseDescriptor descriptor = resultViewer.getSelected();
				if (descriptor != null)
					App.openEditor(descriptor);
			}
		});

		searchButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				search();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no action on default selection
			}

		});

		searchText.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == 13) {
					search();
				}
			}

		});
	}

	/**
	 * Searches for components matching the entered information
	 */
	private void search() {
		final List<BaseDescriptor> results = new ArrayList<>();
		final String searchPhrase = searchText.getText();
		final String clazz = nameToClass.get(modelTypeViewer.getText());
		// if "all databases" is selected
		if (dbCombo.getSelectionIndex() == 0) {
			// for each database
			for (int i = 0; i < databases.length; i++) {
				final IDatabase database = databases[i];
				final IModelComponent[] matching = search(searchPhrase, clazz,
						database);
				// for each matching component
				for (final IModelComponent modelComponent : matching) {
					// add search result
					results.add(new SearchResult(modelComponent, database));
				}
			}
		} else {
			final IDatabase database = databases[dbCombo.getSelectionIndex() - 1];
			final IModelComponent[] matching = search(searchPhrase, clazz,
					database);
			// for each matching component
			for (final IModelComponent modelComponent : matching) {
				// add search result
				results.add(new SearchResult(modelComponent, database));
			}
		}
		// update result viewer
		resultViewer
				.setInput(results.toArray(new SearchResult[results.size()]));

	}

	/**
	 * Searches for model components with the given class containing the search
	 * phrase
	 * 
	 * @param searchPhrase
	 *            the phrase to search for
	 * @param searchClass
	 *            the class of the possible results
	 * @param database
	 *            The database
	 * @return the found model components of class [searchClass] containing the
	 *         search phrase
	 */
	private IModelComponent[] search(final String searchPhrase,
			final String searchClass, final IDatabase database) {
		IModelComponent[] result = new IModelComponent[0];
		// if something was entered
		if (searchPhrase != null && !searchPhrase.equals("")) {
			final List<IModelComponent> components = new ArrayList<>();
			// if a search class was specified
			if (searchClass != null) {
				try {
					// load model component descriptors
					final IModelComponent[] objs = new IModelComponent[0];
					// TODO:

					// for each descriptor
					for (final IModelComponent c : objs) {
						// add to list
						components.add(c);
					}
				} catch (final Exception e) {
					log.error("Searches model components by class failed", e);
				}
			} else {
				try {
					// for each model component class
					for (final String clazz : nameToClass.values()) {
						// load descriptors
						final IModelComponent[] objs = new IModelComponent[0];

						// database
						// .selectDescriptors(Class.forName(clazz));

						// for each descriptor
						for (final IModelComponent c : objs) {
							// add to list
							components.add(c);
						}
					}
				} catch (final Exception e) {
					log.error("Searches model components failed", e);
				}
			}

			final List<IModelComponent> matchedObjects = new ArrayList<>();
			// build a term from the search phrase
			final Term term = TermBuilder.buildTerm(searchPhrase);

			// for each model component
			for (final IModelComponent modelComponent : components) {
				// if model component name matches the search phrase
				if (term.fulfills(modelComponent.getName())) {
					// add to result list
					matchedObjects.add(modelComponent);
				}
			}

			result = matchedObjects.toArray(new IModelComponent[matchedObjects
					.size()]);
		}
		return result;
	}

	public void clear() {
		searchText.clearSelection();
		searchText.setText("");
		resultViewer.setInput(new Object[0]);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// create form and body
		final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		final ScrolledForm scrolledForm = toolkit.createScrolledForm(parent);
		final Composite body = scrolledForm.getBody();
		body.setLayout(new FillLayout());
		toolkit.paintBordersFor(body);

		// create a composite
		final Composite composite = toolkit.createComposite(body, SWT.NONE);
		composite.setLayout(new GridLayout());
		toolkit.paintBordersFor(composite);

		// create composite for database selection combo
		final Composite composite1 = toolkit.createComposite(composite,
				SWT.NONE);
		composite1.setLayout(new GridLayout(2, false));
		composite1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.paintBordersFor(composite1);

		toolkit.createLabel(composite1, Messages.Database);
		// create a combo for selecting a database
		dbCombo = new Combo(composite1, SWT.READ_ONLY);
		dbCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		dbCombo.setSize(150, dbCombo.getSize().y);
		dbCombo.setItems(new String[] { Messages.AllDatabases });
		dbCombo.select(0);

		toolkit.createLabel(composite1, Messages.ObjectType);
		// create a combo for selecting a component type (class)
		modelTypeViewer = new Combo(composite1, SWT.READ_ONLY);
		modelTypeViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
				false));
		modelTypeViewer.setSize(150, dbCombo.getSize().y);

		// load model components from extension registry
		final IExtensionRegistry extensionRegistry = Platform
				.getExtensionRegistry();
		final IConfigurationElement[] elements = extensionRegistry
				.getConfigurationElementsFor("org.openlca.core.model.components");

		final List<String> names = new ArrayList<>();

		// for each found extension
		for (final IConfigurationElement element : elements) {
			// add class name
			final String clazz = element.getAttribute("class");
			if (clazz != null && element.getChildren("category").length > 0) {
				final IConfigurationElement catElement = element
						.getChildren("category")[0];
				final String name = catElement.getAttribute("name");
				nameToClass.put(name, clazz);
				names.add(name);
			}
		}

		// sort class names
		Collections.sort(names, new Comparator<String>() {

			@Override
			public int compare(final String o1, final String o2) {
				return o1.toLowerCase().compareTo(o2.toLowerCase());
			}

		});
		names.add(0, Messages.AllTypes);

		modelTypeViewer.setItems(names.toArray(new String[names.size()]));
		modelTypeViewer.select(0);

		toolkit.adapt(modelTypeViewer);

		toolkit.createLabel(composite1, Messages.SearchTerm);

		// create text field to enter a search phrase
		searchText = toolkit.createText(composite1, "");
		searchText
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		searchText.setSize(150, dbCombo.getSize().y);

		toolkit.createLabel(composite1, "");
		searchButton = toolkit.createButton(composite1, Messages.Search,
				SWT.NONE);

		// create table viewer for displaying the search results
		resultViewer = new TableViewer(composite);
		resultViewer.getTable().setLinesVisible(true);
		resultViewer.getTable().setHeaderVisible(true);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.widthHint = 150;
		resultViewer.getTable().setLayoutData(gd);

		resultViewer.setContentProvider(ArrayContentProvider.getInstance());
		resultViewer.setLabelProvider(new ResultLabelProvider());
		sorter = new ResultSorter();
		resultViewer.setSorter(sorter);

		// create table columns
		final TableColumn c1 = new TableColumn(resultViewer.getTable(),
				SWT.NULL);
		c1.setText(Messages.Name);
		final TableColumn c2 = new TableColumn(resultViewer.getTable(),
				SWT.NULL);
		c2.setText(Messages.Category);
		final TableColumn c3 = new TableColumn(resultViewer.getTable(),
				SWT.NULL);
		c3.setText(Messages.Description);
		final TableColumn c4 = new TableColumn(resultViewer.getTable(),
				SWT.NULL);
		c4.setText(Messages.Database);

		resultViewer.getTable().setSortColumn(c1);
		resultViewer.getTable().setSortDirection(SWT.DOWN);

		resultViewer.setColumnProperties(new String[] { Messages.Name,
				Messages.Category, Messages.Description, Messages.Database });

		// create drag and drop support
		final Transfer transferAgent = ModelTransfer.getInstance();
		final Transfer[] transfers = new Transfer[] { transferAgent };

		final DragSource dragSource = new DragSource(resultViewer.getTable(),
				DND.DROP_MOVE | DND.DROP_DEFAULT);
		dragSource.setTransfer(transfers);
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragSetData(final DragSourceEvent event) {
				if (transferAgent.isSupportedType(event.dataType)) {
					if (resultViewer.getTable().getSelectionCount() > 0) {
						final Object[] modelComponents = new Object[resultViewer
								.getTable().getSelectionCount() + 1];
						IDatabase database = null;
						String componentClass = null;
						for (int i = 0; i < resultViewer.getTable()
								.getSelectionCount(); i++) {
							final SearchResult result = (SearchResult) resultViewer
									.getTable().getSelection()[i].getData();
							if (componentClass == null) {
								componentClass = result.getModelComponent()
										.getClass().getCanonicalName();
								database = result.getDatabase();
							}
							if (database != null) {
								if (!(componentClass.equals(result
										.getModelComponent().getClass()
										.getCanonicalName()) && database
										.equals(result.getDatabase()))) {
									database = null;
									break;
								}
							}
							modelComponents[i] = result.getModelComponent();
						}
						if (database != null) {
							modelComponents[modelComponents.length - 1] = database;
							event.data = modelComponents;
						}
					}
				}
			}

			@Override
			public void dragStart(final DragSourceEvent event) {
				if (resultViewer.getTable().getSelection() != null
						&& resultViewer.getTable().getSelection().length > 0) {
					event.doit = true;
				} else {
					event.doit = false;
				}
			}
		});

		initListeners();

		doRefresh();
		resultViewer.getTable().setSize(150,
				resultViewer.getTable().getSize().y);
	}

	private void doRefresh() {
		NavigationRoot root = Navigator.getNavigationRoot();
		if (root == null)
			return;
		databases = new IDatabase[0];
		String[] names = new String[databases.length];
		for (int i = 0; i < databases.length; i++)
			names[i] = databases[i].getName();
		Arrays.sort(names);
		String[] display = new String[names.length + 1];
		display[0] = Messages.AllDatabases;
		for (int i = 0; i < databases.length; i++)
			display[i + 1] = names[i];
		dbCombo.setItems(display);
		dbCombo.select(0);
	}

	private class ResultLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			Image img = null;
			final IModelComponent component = ((SearchResult) element)
					.getModelComponent();
			if (columnIndex == 0) {
				if (component.getClass() == Flow.class) {
					img = ImageType.FLOW_ICON.get();
				} else if (component.getClass() == FlowProperty.class) {
					img = ImageType.FLOW_PROPERTY_ICON.get();
				} else if (component.getClass() == LCIAMethod.class) {
					img = ImageType.LCIA_ICON.get();
				} else if (component.getClass() == Process.class) {
					img = ImageType.PROCESS_ICON.get();
				} else if (component.getClass() == ProductSystem.class) {
					img = ImageType.PRODUCT_SYSTEM_ICON.get();
				} else if (component.getClass() == UnitGroup.class) {
					img = ImageType.UNIT_GROUP_ICON.get();
				} else if (component.getClass() == Actor.class) {
					img = ImageType.ACTOR_ICON.get();
				} else if (component.getClass() == Source.class) {
					img = ImageType.SOURCE_ICON.get();
				} else if (component.getClass() == Project.class) {
					img = ImageType.PROJECT_ICON.get();
				} else if (component.getClass() == LCIAResult.class) {
					img = ImageType.EXPRESSION_ICON.get();
				}
			}
			return img;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			SearchResult result = (SearchResult) element;
			switch (columnIndex) {
			case Column.NAME:
				return result.getModelComponent().getName();
			case Column.CATEGORY:
				return result.getCategoryPath();
			case Column.DESCRIPTION:
				return result.getModelComponent().getDescription();
			case Column.DATABASE:
				return result.getDatabase().getName();
			default:
				return "";
			}
		}
	}

}
