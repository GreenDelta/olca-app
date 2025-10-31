package org.openlca.app.components;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Parameter;
import org.openlca.util.Strings;

public class FormulaCellEditor extends TextCellEditor {

	private final ContentProposalAdapter proposer;
	private boolean hasFocus;

	private String oldValue;
	private Object selectedElement = null;
	private BiConsumer<Object, String> editFn;

	public FormulaCellEditor(TableViewer table, Supplier<List<Parameter>> locals) {
		super(table.getTable());
		proposer = ParameterProposals.on(text, locals);
		proposer.addContentProposalListener(new IContentProposalListener2() {
			@Override
			public void proposalPopupOpened(ContentProposalAdapter a) {
			}

			@Override
			public void proposalPopupClosed(ContentProposalAdapter a) {
				if (!hasFocus) {
					focusLost();
				}
			}
		});
		getControl().addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				hasFocus = false;
			}

			@Override
			public void focusGained(FocusEvent e) {
				hasFocus = true;
			}
		});
	}

	public void onEdited(BiConsumer<Object, String> editFn) {
		this.editFn = editFn;
	}

	@Override
	protected void focusLost() {
		if (proposer.isProposalPopupOpen())
			return;
		if (proposer.hasProposalPopupFocus())
			return;
		// Focus lost deactivates the cell editor.
		// This must not happen if focus lost was caused by activating
		// the completion proposal popup.
		if (editFn != null) {
			Object value = getValue();
			String formula = value == null ? "" : value.toString();
			if (!formula.equals(oldValue)) {
				editFn.accept(selectedElement, formula);
			}
		}
		selectedElement = null;
		super.focusLost();
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
		var formula = switch (value) {
			case Parameter p -> p.formula;
			case Exchange e -> Strings.isNotBlank(e.formula)
				? e.formula
				: Double.toString(e.amount);
			case ImpactFactor f -> Strings.isNotBlank(f.formula)
				? f.formula
				: Double.toString(f.value);
			case null, default -> null;
		};
		formula = formula == null ? "" : formula;
		super.doSetValue(formula);
		oldValue = formula;
	}
}
