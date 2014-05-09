package org.openlca.app.editors.reports;

import org.openlca.core.model.Project;
import org.openlca.core.model.RootEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Report extends RootEntity {

	private String title;
	private Project project;
	private List<ReportSection> sections = new ArrayList<>();

	@Override
	public Report clone() {
		Report clone = new Report();
		clone.setTitle(getTitle());
		clone.setDescription(getDescription());
		clone.setName(getName());
		clone.setRefId(UUID.randomUUID().toString());
		clone.setProject(getProject());
		for(ReportSection section : getSections())
			clone.getSections().add(section.clone());
		return clone;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

	public List<ReportSection> getSections() {
		return sections;
	}
}
