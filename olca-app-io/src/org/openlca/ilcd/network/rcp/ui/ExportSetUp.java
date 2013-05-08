package org.openlca.ilcd.network.rcp.ui;

import java.util.ArrayList;
import java.util.List;

public class ExportSetUp {

	private String url;
	private String user;
	private String password;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	private List<ExportTupel> exportTupels = new ArrayList<>();

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<ExportTupel> getExportTupels() {
		return exportTupels;
	}

}
