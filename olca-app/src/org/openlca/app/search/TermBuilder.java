/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.search;

/**
 * Creates a new Term out of a given phrase
 * 
 * @author Sebastian Greve
 * 
 */
class TermBuilder {

	/**
	 * Clean up the phrase
	 * 
	 * @param phrase
	 *            The phrase to be cleaned
	 * @return The cleaned phrase
	 */
	private static String clean(String phrase) {
		// if amount of " mod 2 != 0 remove last "
		int count = 0;
		int pos = 0;
		for (int i = 0; i < phrase.length(); i++) {
			if (phrase.charAt(i) == '\"') {
				count++;
				pos = i;
			}
		}
		if (count % 2 != 0) {
			if (pos == 0) {
				phrase = phrase.substring(1);
			} else if (pos == phrase.length() - 1) {
				phrase = phrase.substring(0, phrase.length() - 1);
			} else {
				phrase = phrase.substring(0, pos)
						+ phrase.substring(pos + 1, phrase.length());
			}
		}

		// temporaly replace ' ' with '|empty_space|' for phrases in ""
		String newPhrase = phrase;
		if (phrase.contains("\"")) {
			newPhrase = "";
			while (phrase.contains("\"")) {
				newPhrase += phrase.substring(0, phrase.indexOf('\"'));
				String subPhrase = phrase.substring(phrase.indexOf('\"') + 1);
				subPhrase = subPhrase.substring(0, subPhrase.indexOf('\"'));
				newPhrase += subPhrase.replace(" ", "|empty_space|");
				phrase = phrase.substring(phrase.indexOf('\"') + 1);
				if (phrase.length() > phrase.indexOf('\"')) {
					phrase = phrase.substring(phrase.indexOf('\"') + 1);
				} else {
					phrase = "";
				}
			}
		}

		newPhrase = newPhrase.toLowerCase();
		// remove white spaces at the beginning
		while (newPhrase.startsWith(" ")) {
			if (newPhrase.length() > 1) {
				newPhrase = newPhrase.substring(1);
			} else {
				newPhrase = "";
			}
		}
		// remove white spaces at the end
		while (newPhrase.endsWith(" ")) {
			if (newPhrase.length() > 1) {
				newPhrase = newPhrase.substring(0, newPhrase.length() - 1);
			} else {
				newPhrase = "";
			}
		}

		// append * at the end of each word (implicit)
		final String[] words = newPhrase.split(" ");
		String last = "";
		for (int i = 0; i < words.length; i++) {
			final String word = words[i];
			// do not append for operators
			if (!((word.equals("and") || word.equals("or")) && !(last
					.equals("and") || last.equals("or")))) {
				if (!(word.equals("not") && (last.equals("and")
						|| last.equals("or") || last.equals("")))) {
					if (!word.endsWith("*")) {
						words[i] = word + "*";
					}
				}
			}
			last = word;
		}
		newPhrase = "";
		for (int i = 0; i < words.length; i++) {
			newPhrase += words[i];
			if (i != words.length - 1) {
				newPhrase += " ";
			}
		}
		return newPhrase;
	}

	/**
	 * Builds a term for the given phrase
	 * 
	 * @param phrase
	 *            The phrase a term should be build for
	 * @return The term created for the given phrase
	 */
	public static Term buildTerm(String phrase) {
		phrase = clean(phrase);
		Term term = null;
		if (phrase.length() > 0) {
			final boolean not = phrase.startsWith("not ");
			// if starts with not
			if (not) {
				phrase = phrase.substring(4);
			}
			int closeParenthesis = -1;
			if (phrase.startsWith("(")) {
				int openCounter = 1;
				int actPos = 1;
				// count open and close parenthesis
				while (closeParenthesis == -1) {
					if (phrase.charAt(actPos) == '(') {
						openCounter++;
					} else if (phrase.charAt(actPos) == ')') {
						openCounter--;
						if (openCounter == 0) {
							closeParenthesis = actPos;
						}
					}
					actPos++;
				}
				// if the last closing parenthesis is not at the end of the
				// phrase
				if (closeParenthesis != phrase.length() - 1) {
					// get part until close parenthesis
					final String part1 = phrase.substring(1, closeParenthesis);
					// get rest
					final String rest = phrase.substring(closeParenthesis + 1);
					if (rest.startsWith(" and ")) {

						// build and term
						Term term1 = null;
						final Term term2 = buildTerm(rest.substring(5));
						if (not) {
							term1 = new NotTerm(buildTerm(part1));
						} else {
							term1 = buildTerm(part1);
						}
						term = new AndTerm(term1, term2);

					} else if (rest.startsWith(" or ")) {

						// build or term
						Term term1 = null;
						final Term term2 = buildTerm(rest.substring(4));
						if (not) {
							term1 = new NotTerm(buildTerm(part1));
						} else {
							term1 = buildTerm(part1);
						}
						term = new OrTerm(term1, term2);

					} else {

						// build and term
						Term term1 = null;
						final Term term2 = buildTerm(rest.substring(1));
						if (not) {
							term1 = new NotTerm(buildTerm(part1));
						} else {
							term1 = buildTerm(part1);
						}
						term = new AndTerm(term1, term2);

					}
				} else {
					if (not) {
						// build not term
						term = new NotTerm(buildTerm(phrase.substring(1,
								phrase.length() - 1)));
					} else {
						// build term
						term = buildTerm(phrase.substring(1,
								phrase.length() - 1));
					}
				}
			} else {
				// if contains or and not ( before the or
				if (phrase.contains(" or ")
						&& (!phrase.contains("(") || phrase.indexOf("(") > phrase
								.indexOf(" or "))) {
					String part1 = phrase.substring(0, phrase.indexOf(" or "));
					// remove white spaces
					while (part1.endsWith(" ")) {
						if (part1.length() > 1) {
							part1 = part1.substring(0, part1.length() - 1);
						} else {
							part1 = "";
						}
					}
					String part2 = phrase.substring(phrase.indexOf(" or ") + 4);
					// remove white spaces
					while (part2.startsWith(" ")) {
						if (part2.length() > 1) {
							part2 = part2.substring(1);
						} else {
							part2 = "";
						}
					}

					// build sub terms
					Term term1 = new EmptyTerm();
					if (!part1.equals("")) {
						if (not) {
							term1 = new NotTerm(buildTerm(part1));
						} else {
							term1 = buildTerm(part1);
						}
					}
					Term term2 = new EmptyTerm();
					if (!part2.equals("")) {
						term2 = buildTerm(part2);
					}

					term = new OrTerm(term1, term2);

				} else if (phrase.contains(" and ")) {

					String part1 = phrase.substring(0, phrase.indexOf(" and "));
					// remove white spaces
					while (part1.endsWith(" ")) {
						if (part1.length() > 1) {
							part1 = part1.substring(0, part1.length() - 1);
						} else {
							part1 = "";
						}
					}
					String part2 = phrase
							.substring(phrase.indexOf(" and ") + 5);
					// remove white spaces
					while (part2.startsWith(" ")) {
						if (part2.length() > 1) {
							part2 = part2.substring(1);
						} else {
							part2 = "";
						}
					}

					// build sub terms
					Term term1 = new EmptyTerm();
					if (!part1.equals("")) {
						if (not) {
							term1 = new NotTerm(buildTerm(part1));
						} else {
							term1 = buildTerm(part1);
						}
					}
					Term term2 = new EmptyTerm();
					if (!part2.equals("")) {
						term2 = buildTerm(part2);
					}

					term = new AndTerm(term1, term2);

				} else if (phrase.contains(" ")) {

					final String part1 = phrase.substring(0,
							phrase.indexOf(' '));
					final String part2 = phrase
							.substring(phrase.indexOf(' ') + 1);

					// build sub terms
					Term term1 = null;
					if (not) {
						term1 = new NotTerm(buildTerm(part1));
					} else {
						term1 = buildTerm(part1);
					}
					final Term term2 = buildTerm(part2);
					term = new AndTerm(term1, term2);

				} else {
					if (not) {
						term = new NotTerm(new AtomicTerm(phrase.replace(
								"|empty_space|", " ")));
					} else {
						term = new AtomicTerm(phrase.replace("|empty_space|",
								" "));
					}
				}
			}
		} else {
			term = new EmptyTerm();
		}
		return term;
	}
}
