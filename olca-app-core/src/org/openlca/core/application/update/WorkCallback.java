package org.openlca.core.application.update;

public interface WorkCallback {

	/**
	 * Report a new part done. Not the total!
	 * 
	 * @param partDone
	 */
	void report(int partDone);

	void increaseTotal(int newTotal);

}
