package org.openlca.app.collaboration.util;

public interface Constants {

	public static String DEFAULT_REMOTE = "origin";
	public static String DEFAULT_BRANCH = "main";
	public static String LOCAL_BRANCH = "HEAD";
	public static String LOCAL_REF = "refs/heads/" + DEFAULT_BRANCH;
	public static String REMOTE_BRANCH = DEFAULT_REMOTE + "/" + DEFAULT_BRANCH;
	public static String REMOTE_REF = "refs/remotes/" + REMOTE_BRANCH;
	public static String DEFAULT_FETCH_SPEC = "+" + Constants.LOCAL_REF + ":" + Constants.REMOTE_REF;

}
