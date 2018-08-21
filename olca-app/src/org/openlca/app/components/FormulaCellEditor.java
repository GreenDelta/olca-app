package org.openlca.app.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.openlca.app.db.Database;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.util.Strings;

public class FormulaCellEditor extends TextCellEditor {

	private final Supplier<List<Parameter>> parameters;

	private String oldValue;
	private boolean isPopupOpen = false;
	private Object selectedElement = null;
	private BiConsumer<Object, String> editFn;

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
		// Otherwise, the ColumnViewerEditor will install an additional focus listener
		// that cancels cell editing on focus lost, even if focus gets lost due to
		// activation of the completion proposal popup. See also bug 58777.
		return false;
	}

	@Override
	protected void doSetValue(Object value) {
		selectedElement = value;
		String formula = null;
		if (value instanceof Parameter) {
			formula = ((Parameter) value).getFormula();
		}
		formula = formula == null ? "" : formula;
		super.doSetValue(formula);
		oldValue = formula;
	}

	private class ProposalProvider implements IContentProposalProvider {

		private List<Parameter> globals;

		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			String prefix = getPrefix(contents, position);
			if (prefix == null)
				return new IContentProposal[0];
			return proposalsFor(prefix);
		}

		private String getPrefix(String contents, int position) {
			if (contents == null || position == 0)
				return "";
			if (position > contents.length())
				return "";
			int pos = position - 1;
			StringBuilder buffer = new StringBuilder();
			boolean hasLetters = false;
			while (pos >= 0) {
				char c = contents.charAt(pos);
				if (Character.isWhitespace(c) || !Character.isJavaIdentifierPart(c))
					break;
				if (!hasLetters) {
					hasLetters = Character.isLetter(c);
				}
				buffer.append(c);
				pos--;
			}
			if (buffer.length() == 0)
				return "";
			if (!hasLetters)
				return null;
			return buffer.reverse().toString().toLowerCase();
		}

		private IContentProposal[] proposalsFor(String prefix) {
			List<Proposal> proposals = new ArrayList<>();
			HashSet<String> names = new HashSet<>();
			if (parameters != null) {
				for (Parameter p : parameters.get()) {
					Proposal proposal = newProposal(names, p, prefix);
					if (proposal != null) {
						proposals.add(proposal);
					}
				}
			}
			if (globals == null) {
				globals = new ParameterDao(
						Database.get()).getGlobalParameters();
			}
			for (Parameter p : globals) {
				Proposal proposal = newProposal(names, p, prefix);
				if (proposal != null) {
					proposals.add(proposal);
				}
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}

		private Proposal newProposal(HashSet<String> existing, Parameter p, String prefix) {
			if (p == null || p.getName() == null || existing.contains(p.getName()))
				return null;
			String lname = p.getName().toLowerCase();
			if (!lname.startsWith(prefix))
				return null;
			String replacement = p.getName().substring(prefix.length()) + " ";
			return new Proposal(replacement, p);
		}
	}

	private class Proposal implements IContentProposal {

		final String replacement;
		final Parameter parameter;

		Proposal(String replacement, Parameter parameter) {
			this.replacement = replacement;
			this.parameter = parameter;
		}

		@Override
		public String getContent() {
			return replacement;
		}

		@Override
		public int getCursorPosition() {
			return replacement.length() + 1;
		}

		@Override
		public String getLabel() {
			return parameter.getName();
		}

		@Override
		public String getDescription() {
			return parameter.getDescription();
		}
	}
}
