package org.openlca.app.components;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Parameter;
import org.openlca.util.Strings;

public class FormulaCellEditor extends TextCellEditor {

	private String oldValue;
	private boolean isPopupOpen = false;
	private Object selectedElement = null;
	private BiConsumer<Object, String> editFn;

	public FormulaCellEditor(TableViewer table, Supplier<List<Parameter>> locals) {
		super(table.getTable());
		ContentProposalAdapter adapter = ParameterProposals.on(text, locals);
		adapter.addContentProposalListener(new IContentProposalListener2() {
			@Override
			public void proposalPopupOpened(ContentProposalAdapter adapter) {
				isPopupOpen = true;
			}

			@Override
			public void proposalPopupClosed(ContentProposalAdapter adapter) {
				isPopupOpen = false;
			}
		});
	}

	public void onEdited(BiConsumer<Object, String> editFn) {
		this.editFn = editFn;
	}

	@Override
	protected void focusLost() {
		if (!isPopupOpen) {
			// Focus lost deactivates the cell editor.
			// This must not happen if focus lost was caused by activating
			// the completion proposal popup.
			if (editFn != null) {
				Object value = getValue();
				String formula = value == null ? "" : value.toString();
				if (!Strings.nullOrEqual(oldValue, formula)) {
					editFn.accept(selectedElement, formula);
				}
			}
			selectedElement = null;
			super.focusLost();
		}
	}

	@Override
	protected boolean dependsOnExternalFocusListener() {
		// Always return false;
		// Otherwise, the ColumnViewerEditor will install an additional focus
		// listener that cancels cell editing on focus lost, even if focus
		// gets lost due to activation of the completion proposal popup.
		// See also bug 58777.
		return false;
	}

	@Override
	protected void doSetValue(Object value) {
		selectedElement = value;
		String formula = null;
		if (value instanceof Parameter) {
			formula = ((Parameter) value).getFormula();
		} else if (value instanceof Exchange) {
			Exchange e = (Exchange) value;
			if (Strings.notEmpty(e.amountFormula)) {
				formula = e.amountFormula;
			} else {
				formula = Double.toString(e.amount);
			}
		} else if (value instanceof ImpactFactor) {
			ImpactFactor f = (ImpactFactor) value;
			if (Strings.notEmpty(f.formula)) {
				formula = f.formula;
			} else {
				formula = Double.toString(f.value);
			}
		}
		formula = formula == null ? "" : formula;
		super.doSetValue(formula);
		oldValue = formula;
	}

}
