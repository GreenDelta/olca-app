package org.openlca.app.components;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.openlca.core.model.Parameter;

public class FormulaCellEditor extends TextCellEditor {

	private String formula;

	private final Supplier<List<Parameter>> parameters;
	private boolean isPopupOpen = false;

	public FormulaCellEditor(TableViewer table, Supplier<List<Parameter>> parameters) {
		super(table.getTable());
		this.parameters = parameters;
		ContentProposalAdapter adapter = new ContentProposalAdapter(
				text, new TextContentAdapter(),
				new ProposalProvider(),
				null, null);
		adapter.addContentProposalListener(new IContentProposalListener2() {
			@Override
			public void proposalPopupOpened(ContentProposalAdapter adapter) {
				isPopupOpen = false;
			}

			@Override
			public void proposalPopupClosed(ContentProposalAdapter adapter) {
				isPopupOpen = true;
			}
		});
	}

	@Override
	protected void focusLost() {
		if (!isPopupOpen) {
			// Focus lost deactivates the cell editor.
			// This must not happen if focus lost was caused by activating
			// the completion proposal popup.
			super.focusLost();
		}
	}

	@Override
	protected boolean dependsOnExternalFocusListener() {
		// Always return false;
		// Otherwise, the ColumnViewerEditor will install an additional focus listener
		// that cancels cell editing on focus lost, even if focus gets lost due to
		// activation of the completion proposal popup. See also bug 58777.
		return false;
	}

	@Override
	protected void doSetValue(Object value) {
		if (value == null) {
			this.formula = "";
		} else {
			this.formula = value.toString();
		}
	}

	@Override
	protected Object doGetValue() {
		return formula;
	}

	private class ProposalProvider implements IContentProposalProvider {
		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			if (parameters == null)
				return new IContentProposal[0];
			return parameters.get().stream()
					.map(p -> new Proposal(p))
					.toArray(IContentProposal[]::new);
		}
	}

	private class Proposal implements IContentProposal {

		final Parameter parameter;

		Proposal(Parameter parameter) {
			this.parameter = parameter;
		}

		@Override
		public String getContent() {
			if (parameter.getName() == null)
				return "";
			return parameter.getName();
		}

		@Override
		public int getCursorPosition() {
			String name = getContent();
			return name.length() == 0 ? 0 : name.length() - 1;
		}

		@Override
		public String getLabel() {
			return getContent();
		}

		@Override
		public String getDescription() {
			return parameter.getDescription();
		}

	}

}
