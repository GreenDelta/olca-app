package org.openlca.app.rcp.update;

public interface WorkCallback {

	/**
	 * Report a new part done. Not the total!
	 * 
	 * @param partDone
	 */
	void report(int partDone);

	void increaseTotal(int newTotal);

}
