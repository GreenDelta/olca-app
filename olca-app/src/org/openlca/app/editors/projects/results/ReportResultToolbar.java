package org.openlca.app.editors.projects.results;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;

import com.google.gson.Gson;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Popup;
import org.openlca.io.xls.results.ProjectResultExport;

public class ReportResultToolbar extends EditorActionBarContributor {

	private ProjectResultEditor editor;
	private ReportExportAction reportExport;
	private ExcelExportAction excelExport;

	@Override
	public void setActiveEditor(IEditorPart part) {
		editor = part instanceof ProjectResultEditor
			? (ProjectResultEditor) part
			: null;
		super.setActiveEditor(part);
		activateActions();
	}

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		excelExport = new ExcelExportAction();
		manager.add(excelExport);
		reportExport = new ReportExportAction();
		manager.add(reportExport);
		activateActions();
	}

	private void activateActions() {
		if (reportExport == null || excelExport == null)
			return;
		if (editor == null || editor.data == null) {
			reportExport.setEnabled(false);
			excelExport.setEnabled(false);
			return;
		}
		excelExport.setEnabled(true);
		reportExport.setEnabled(editor.data.hasReport());
	}

	private class ExcelExportAction extends Action {

		ExcelExportAction() {
			setImageDescriptor(Images.descriptor(FileType.EXCEL));
			setToolTipText(M.ExportToExcel);
		}

		@Override
		public void run() {
			if (editor == null || editor.data == null)
				return;
			var data = editor.data;
			var file = FileChooser.forSavingFile(
				"Export project result", "project result.xlsx");
			if (file == null)
				return;
			var export = new ProjectResultExport(
				data.project(), data.result(), data.db());
			try {
				export.writeTo(file);
				Popup.info("Exported results to " + file.getName());
			} catch (Exception e) {
				ErrorReporter.on("Export of project result failed", e);
			}
		}
	}


	private class ReportExportAction extends Action {

		private final String CALL_HOOK = "//{{set_data_call}}";

		public ReportExportAction() {
			setImageDescriptor(Icon.CHART.descriptor());
			setToolTipText(M.ExportReport);
		}

		@Override
		public void run() {
			if (editor == null || editor.data == null || !editor.data.hasReport())
				return;
			var report = editor.data.report();
			var dir = FileChooser.selectFolder();
			if (dir == null)
				return;
			var htmlDir = HtmlFolder.getDir(
				RcpActivator.getDefault().getBundle());
			tryExport(report, dir, htmlDir);
		}

		private void tryExport(Report report, File targetDir, File htmlFolder) {
			try {
				copyLibs(targetDir, htmlFolder);
				String json = new Gson().toJson(report);
				String call = "document.addEventListener(\"DOMContentLoaded\", "
					+ "function() { setData(" + json + "); });";
				var template = Objects.requireNonNull(HtmlFolder.getFile(
					RcpActivator.getDefault().getBundle(), "report.html"));
				var text = new StringBuilder();
				Files.readAllLines(template.toPath(), StandardCharsets.UTF_8)
					.stream().map(line -> line.contains(CALL_HOOK)
					? line.replace(CALL_HOOK, call)
					: line)
					.forEach(line -> {
						text.append(line);
						text.append('\n');
					});

				var fileName = report.title == null
					? "report.html"
					: report.title.replaceAll("\\W+", "_") + ".html";
				var file = new File(targetDir, fileName);
				Files.writeString(file.toPath(), text.toString());
			} catch (Exception e) {
				ErrorReporter.on("failed to export report", e);
			}
		}

		private void copyLibs(File targetDir, File htmlFolder)
			throws IOException {
			File lib = new File(htmlFolder, "report.js");
			if (lib.exists()) {
				File tlib = new File(targetDir, "report.js");
				Files.copy(lib.toPath(), tlib.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			}
			File libDir = new File(htmlFolder, "lib");
			if (!libDir.exists())
				return;
			File targetLibDir = new File(targetDir, "lib");
			if (!targetLibDir.exists()) {
				Files.createDirectories(targetLibDir.toPath());
			}
			FileUtils.copyDirectory(libDir, targetLibDir);
		}
	}
}
