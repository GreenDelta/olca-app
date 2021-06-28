package org.openlca.app.editors.projects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.Gson;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Project;

public class ProjectEditor extends ModelEditor<Project> {

	public static String ID = "editors.project";
	Report report;

	public ProjectEditor() {
		super(Project.class);
	}

	public Report report() {
		return report;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		var reportFile = reportFile();
		if (reportFile.exists()) {
			try (var stream = new FileInputStream(reportFile);
					 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
					 var buffer = new BufferedReader(reader)) {
				report = new Gson().fromJson(buffer, Report.class);
			} catch (IOException e) {
				ErrorReporter.on("Failed to open report file: " + reportFile, e);
			}
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProjectSetupPage(this));
			if (report != null) {
				addPage(new ReportEditorPage(this));
			}
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("Failed to add project pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (report != null) {
			try {
				var file = reportFile();
				var parent = file.getParentFile();
				if (!parent.exists()) {
					Files.createDirectories(parent.toPath());
				}
				Files.writeString(reportFile().toPath(), report.toJson());
			} catch (IOException e) {
				ErrorReporter.on("Failed to write report file", e);
			}
		}
		super.doSave(monitor);
	}

	private File reportFile() {
		var dir = DatabaseDir.getDir(getModel());
		return new File(dir, "report.json");
	}
}
