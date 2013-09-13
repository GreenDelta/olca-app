package org.openlca.app.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.CostCategory;
import org.openlca.core.model.ProductCostEntry;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dialog for editing cost entries in process data sets. */
class ProcessCostEntryDialog extends Dialog {

	private FormToolkit toolkit;

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	private List<ProductCostEntry> existingEntries;
	private CostCategory[] comboItems;

	private Combo combo;
	private Text text;
	private Button fix;
	private boolean createNew = false;
	private ProductCostEntry newEntry;

	private Label label;

	public ProcessCostEntryDialog(Shell parentShell, IDatabase database,
			List<ProductCostEntry> existingEntries) {
		super(parentShell);
		toolkit = new FormToolkit(parentShell.getDisplay());
		this.database = database;
		this.existingEntries = existingEntries;
		newEntry = new ProductCostEntry();
	}

	@Override
	public void okPressed() {
		if (!createNew) {
			int i = combo.getSelectionIndex();
			if (i < 0 || i >= comboItems.length)
				Error.showBox("No cost category selected");
			else {
				newEntry.setCostCategory(comboItems[i]);
				super.okPressed();
			}
		} else {
			String newName = text.getText().trim();
			if (!newNameOk(newName))
				Error.showBox("The name is empty or already exists");
			else {
				createCostCategory(newName);
				super.okPressed();
			}
		}
	}

	private boolean newNameOk(String newName) {
		if (newName.isEmpty())
			return false;
		try {
			List<CostCategory> all = database.createDao(CostCategory.class)
					.getAll();
			for (CostCategory costCat : all) {
				if (newName.equalsIgnoreCase(costCat.getName()))
					return false;
			}
			return true;
		} catch (Exception e) {
			log.error("Could not load cost categories", e);
			return false;
		}
	}

	private boolean createCostCategory(String newName) {
		try {
			CostCategory cat = new CostCategory();
			cat.setName(newName);
			cat.setFix(fix.getSelection());
			database.createDao(CostCategory.class).insert(cat);
			newEntry.setCostCategory(cat);
			return true;
		} catch (Exception e) {
			log.error("Could not create cost category", e);
			return false;
		}
	}

	public ProductCostEntry getCostEntry() {
		return newEntry;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		getShell().pack();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	@Override
	public boolean close() {
		if (toolkit != null)
			toolkit.dispose();
		return super.close();
	}

	@Override
	protected Control createDialogArea(Composite root) {
		getShell().setText("New cost entry");
		toolkit.adapt(root);
		Composite area = (Composite) super.createDialogArea(root);
		toolkit.adapt(area);
		Composite container = toolkit.createComposite(area);
		UI.gridData(container, true, true);
		fillContainer(container);
		getShell().pack();
		UI.center(getParentShell(), getShell());
		return area;
	}

	private void fillContainer(Composite container) {
		UI.gridLayout(container, 3);
		label = UI.formLabel(container, toolkit, "Cost category");
		Composite stack = toolkit.createComposite(container);
		UI.gridData(stack, true, false).widthHint = 250;
		StackLayout stackLayout = new StackLayout();
		stack.setLayout(stackLayout);
		combo = createCombo(stack);
		stackLayout.topControl = combo;
		text = toolkit.createText(stack, "");
		createLink(container, stackLayout);
		Text amountText = UI.formText(container, toolkit, "Amount");
		UI.formLabel(container, toolkit, ""); // placeholder
		UI.formLabel(container, toolkit, "Fixed costs");
		fix = toolkit.createButton(container, null, SWT.CHECK);
		fix.setEnabled(false);
		fix.setSelection(combo.getItemCount() > 0 ? comboItems[0].isFix()
				: false);
		new DataBinding().onDouble(newEntry, "amount", amountText);
	}

	private Combo createCombo(Composite stack) {
		Combo combo = new Combo(stack, SWT.READ_ONLY);
		toolkit.adapt(combo);
		comboItems = initComboItems();
		String[] itemLabels = new String[comboItems.length];
		for (int i = 0; i < comboItems.length; i++)
			itemLabels[i] = comboItems[i].getName();
		combo.setItems(itemLabels);
		if (itemLabels.length > 0)
			combo.select(0);
		combo.addSelectionListener(new ComboSelectionListener());
		return combo;
	}

	private void createLink(final Composite container,
			final StackLayout stackLayout) {
		final ImageHyperlink link = new ImageHyperlink(container, SWT.TOP);
		toolkit.adapt(link);
		link.setImage(ImageType.ADD_ICON.get());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				createNew = !createNew;
				if (createNew) {
					stackLayout.topControl = text;
					link.setImage(ImageType.DELETE_ICON.get());
					label.setText("New category");
					fix.setEnabled(true);
				} else {
					stackLayout.topControl = combo;
					link.setImage(ImageType.ADD_ICON.get());
					label.setText("Cost category");
					fix.setEnabled(false);
				}
				getShell().layout(true, true);
				getShell().pack();
			}
		});
	}

	private CostCategory[] initComboItems() {
		try {
			List<CostCategory> all = database.createDao(CostCategory.class)
					.getAll();
			List<CostCategory> items = new ArrayList<>();
			for (CostCategory costCat : all) {
				if (!entryExists(costCat))
					items.add(costCat);
			}
			Collections.sort(items, new CostCategoryComparator());
			return items.toArray(new CostCategory[items.size()]);
		} catch (Exception e) {
			log.error("Failed to get cost categories", e);
			return new CostCategory[0];
		}
	}

	private boolean entryExists(CostCategory category) {
		for (ProductCostEntry entry : existingEntries) {
			if (category.equals(entry.getCostCategory()))
				return true;
		}
		return false;
	}

	private class CostCategoryComparator implements Comparator<CostCategory> {
		@Override
		public int compare(CostCategory first, CostCategory second) {
			return Strings.compare(first.getName(), second.getName());
		}
	}

	private class ComboSelectionListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			fix.setSelection(comboItems[combo.getSelectionIndex()].isFix());
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			fix.setSelection(comboItems[combo.getSelectionIndex()].isFix());
		}

	}

}
