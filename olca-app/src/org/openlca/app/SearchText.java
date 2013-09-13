package org.openlca.app;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchText extends WorkbenchWindowControlContribution {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Text text;

	@Override
	protected Control createControl(Composite parent) {
		parent.setLayout(new FillLayout());
		log.trace("create search text control");
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		text = new Text(composite, SWT.BORDER | SWT.SEARCH);
		UI.gridData(text, true, false).minimumWidth = 180;
		ToolBar toolBar = new ToolBar(composite, SWT.NONE);
		ToolBarManager manager = new ToolBarManager(toolBar);
		manager.add(new DropDownAction());
		manager.update(true);
		toolBar.pack();
		return composite;
	}

	private class DropDownAction extends Action implements IMenuCreator {

		private Menu menu;
		private ModelType selectedType;

		public DropDownAction() {
			setText("Search");
			setImageDescriptor(ImageType.SEARCH_ICON.getDescriptor());
			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			if (menu == null)
				return;
			if (!menu.isDisposed())
				menu.dispose();
			menu = null;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return null;
		}

		@Override
		public Menu getMenu(Control parent) {
			if (menu != null)
				menu.dispose();
			menu = new Menu(parent);

			//@formatter:off
			String[] texts = {"Search all types",
					"Search in projects",
					"Search in product systems",
					"Search in LCIA methods",
					"Search in processes",
					"Search in flows",
					"Search in flow properties",
					"Search in unit groups",
					"Search in sources",
					"Search in actors"};
			
			ModelType[] types = { null,
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.SOURCE,
				ModelType.ACTOR
			};
			//@formatter:on

			for (int i = 0; i < texts.length; i++)
				createItem(menu, texts[i], types[i]);
			return menu;
		}

		private void createItem(Menu menu, final String text,
				final ModelType type) {
			Image image = null;
			if (type == null)
				image = ImageType.SEARCH_ICON.get();
			else
				image = Images.getIcon(type);
			MenuItem item = new MenuItem(menu, SWT.NONE);
			item.setText(text);
			item.setImage(image);
			item.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateSelection(text, type);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		}

		private void updateSelection(String text, ModelType type) {
			setText(text);
			ImageDescriptor imageDescriptor = null;
			if (type == null)
				imageDescriptor = ImageType.SEARCH_ICON.getDescriptor();
			else
				imageDescriptor = Images.getIconDescriptor(type);
			setImageDescriptor(imageDescriptor);
			selectedType = type;
		}

		public void run() {
			log.trace("run search");
		}

	}
}
