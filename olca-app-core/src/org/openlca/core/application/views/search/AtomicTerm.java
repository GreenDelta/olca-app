/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of term for realising an atomic term (No operation)
 * 
 * @author Sebastian Greve
 * 
 */
class AtomicTerm implements Term {

	/**
	 * The atomic term
	 */
	private final String term;

	/**
	 * Creates a new instance
	 * 
	 * @param term
	 *            The atomic term
	 */
	public AtomicTerm(final String term) {
		this.term = term;
	}

	@Override
	public boolean fulfills(final String p) {
		String phrase = p.toLowerCase();
		final List<String> words = new ArrayList<>();

		// remove white spaces at the beginning
		while (phrase.startsWith(" ")) {
			if (phrase.length() > 1) {
				phrase = phrase.substring(1);
			} else {
				phrase = "";
			}
		}
		// remove white spaces at the end
		while (phrase.endsWith(" ")) {
			if (phrase.length() > 1) {
				phrase = phrase.substring(0, phrase.length() - 1);
			} else {
				phrase = "";
			}
		}

		// split the phrase into words
		while (phrase.contains(" ")) {
			words.add(phrase.substring(0, phrase.indexOf(" ")));
			phrase = phrase.substring(phrase.indexOf(" ") + 1);
		}
		words.add(phrase);

		boolean fulfills = false;
		if (term.startsWith("*") && term.endsWith("*") && term.length() > 2) {
			// check if the given term is part of a word
			int i = 0;
			final String subTerm = term.substring(1, term.length() - 1);
			while (!fulfills && i < words.size()) {
				final String word = words.get(i);
				if (word.contains(subTerm)) {
					fulfills = true;
				}
				i++;
			}
			if (!fulfills) {
				fulfills = p.contains(subTerm);
			}
		} else if (term.startsWith("*") && term.length() > 1) {
			// check if the given term is the end of a word
			int i = 0;
			final String subTerm = term.substring(1);
			while (!fulfills && i < words.size()) {
				final String word = words.get(i);
				if (word.endsWith(subTerm)) {
					fulfills = true;
				}
				i++;
			}
			if (!fulfills) {
				fulfills = p.endsWith(subTerm);
			}
		} else if (term.endsWith("*") && term.length() > 1) {
			// check if the given term is the beginning of a word
			int i = 0;
			final String subTerm = term.substring(0, term.length() - 1);
			while (!fulfills && i < words.size()) {
				final String word = words.get(i);
				if (word.startsWith(subTerm)) {
					fulfills = true;
				}
				i++;
			}
			if (!fulfills) {
				fulfills = p.startsWith(subTerm);
			}
		} else {
			// check if the term is contained in the words
			fulfills = words.contains(term) || p.equals(term);
		}
		return fulfills;
	}
}
