package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.BaseNameComparator;
import org.openlca.util.Strings;

public abstract class AbstractComboViewer<T> extends
		AbstractViewer<T, TableComboViewer> {

	protected AbstractComboViewer(Composite parent) {
		super(parent);
	}

	public abstract Class<T> getType();

	@Override
	protected TableComboViewer createViewer(Composite parent) {
		var combo = new TableCombo(parent,
				SWT.READ_ONLY | SWT.BORDER | SWT.VIRTUAL);
		UI.gridData(combo, true, false).widthHint = 350;
		if (useColumnHeaders()) {
			if (useColumnBounds())
				combo.defineColumns(getColumnHeaders(), getColumnBounds());
			else
				combo.defineColumns(getColumnHeaders());
			combo.setShowTableHeader(true);
			combo.setShowTableLines(true);
		}
		combo.setDisplayColumnIndex(getDisplayColumn());
		var viewer = new TableComboViewer(combo);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setComparator(getComparator());

		// add a key listener that let the selection jump to the
		// first matching element of the combo input when a
		// character key is pressed
		combo.addTextControlKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {

				var label = viewer.getLabelProvider();
				var col = getDisplayColumn();
				var c = Character.toLowerCase(e.character);
				if (!Character.isLetterOrDigit(c))
					return;
				var input = getViewer().getInput();
				if (!(input instanceof Object[]))
					return;

				var objects = (Object[]) input;
				for (var obj : objects) {

					String text = null;
					if (label instanceof ITableLabelProvider) {
						text = ((ITableLabelProvider) label)
								.getColumnText(obj, col);
					} else if (label instanceof ColumnLabelProvider) {
						text = ((ColumnLabelProvider) label)
								.getText(obj);
					}
					if (Strings.nullOrEmpty(text))
						continue;

					var first = Character.toLowerCase(text.charAt(0));
					if (first == c) {
						getViewer().setSelection(new StructuredSelection(obj));
						break;
					}
				}
			}
		});

		return viewer;
	}

	protected ViewerComparator getComparator() {
		return new BaseNameComparator();
	}

	private int[] getColumnBounds() {
		int[] columnBoundsPercentages = getColumnBoundsPercentages();
		int[] columnBoundsAbsolute = new int[columnBoundsPercentages.length];
		// TODO: this is way too large for combos in dialogs
		int total = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell().getSize().x / 2;
		for (int i = 0; i < columnBoundsPercentages.length; i++)
			columnBoundsAbsolute[i] = (int) (total / 100d * columnBoundsPercentages[i]);
		return columnBoundsAbsolute;
	}

	private boolean useColumnHeaders() {
		return getColumnHeaders() != null && getColumnHeaders().length > 0;
	}

	private boolean useColumnBounds() {
		return getColumnBoundsPercentages() != null
				&& getColumnBoundsPercentages().length > 0
				&& getColumnBoundsPercentages().length == getColumnHeaders().length;
	}

	/**
	 * Subclasses may override this for support of column headers for the table
	 * combo, if null or empty array is returned, the headers are not visible and
	 * the combo behaves like a standard combo
	 */
	protected String[] getColumnHeaders() {
		return null;
	}

	/**
	 * Subclasses may override this for support of column header bounds for the
	 * table combo, if null or empty array is returned or the number of array
	 * elements does not match the return value of getColumnHeaders, the headers
	 * will have standard width
	 */
	protected int[] getColumnBoundsPercentages() {
		return null;
	}

	/**
	 * Subclasses may override this method to specify another display column when
	 * using table combo. Defaults to 0
	 */
	protected int getDisplayColumn() {
		return 0;
	}

}
