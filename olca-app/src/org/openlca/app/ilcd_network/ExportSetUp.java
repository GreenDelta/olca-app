package org.openlca.app.ilcd_network;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.Descriptor;

public class ExportSetUp {

	private String url;
	private String user;
	private String password;
	private final List<Descriptor> exportTuples = new ArrayList<>();

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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Descriptor> getExportTuples() {
		return exportTuples;
	}

}
