package org.openlca.app.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.db.Database;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;

public class ParameterProposals implements IContentProposalProvider {

	private final Supplier<List<Parameter>> locals;
	private List<Parameter> globals;

	private ParameterProposals(Supplier<List<Parameter>> locals) {
		this.locals = locals;
	}

	public static ContentProposalAdapter on(Text text) {
		return on(text, null);
	}

	public static ContentProposalAdapter on(Text text, Supplier<List<Parameter>> locals) {
		ContentProposalAdapter adapter = new ContentProposalAdapter(
				text, new TextContentAdapter(),
				new ParameterProposals(locals),
				null, null);
		return adapter;
	}

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
		if (locals != null) {
			for (Parameter p : locals.get()) {
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
		if (p == null || p.name == null || existing.contains(p.name))
			return null;
		String lname = p.name.toLowerCase();
		if (!lname.startsWith(prefix))
			return null;
		String replacement = p.name.substring(prefix.length()) + " ";
		return new Proposal(replacement, p);
	}

	private static class Proposal implements IContentProposal {

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
			return parameter.name;
		}

		@Override
		public String getDescription() {
			return parameter.description;
		}
	}
}