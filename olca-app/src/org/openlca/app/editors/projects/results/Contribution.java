package org.openlca.app.editors.projects.results;

import org.openlca.core.model.descriptors.CategorizedDescriptor;

/**
 * Contains the data of a cell entry in the contribution table.
 */
class Contribution {

	/**
	 * The process or product system of the result contribution. This is
	 * {@code null} when this is the "rest" ("others") contribution.
	 */
	final CategorizedDescriptor process;

	/**
	 * The amount of the result contribution.
	 */
	final double amount;

	/**
	 * {@code true} if this is the "rest" contribution: if there are n processes
	 * and sub-systems in the system and the user selects k results, this contains
	 * the sum of the n-k other contributions.
	 */
	final boolean isRest;

	Contribution(CategorizedDescriptor process, double amount) {
		this.process = process;
		this.amount = amount;
		this.isRest = process == null;
	}

	static Contribution restOf(double amount) {
		return new Contribution(null, amount);
	}

}
