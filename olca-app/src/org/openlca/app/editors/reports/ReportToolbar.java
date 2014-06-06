package org.openlca.app.editors.reports;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.html.HtmlFolder;
import org.openlca.app.html.HtmlView;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ReportToolbar extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		Action previewAction = Actions.create(
				"Preview",
				ImageType.SEARCH_ICON.getDescriptor(), () -> {
					Report report = getReport();
					if (report != null)
						ReportViewer.open(getReport());
				});
		manager.add(previewAction);
		manager.add(new ExportAction());
	}

	private Report getReport() {
		try {
			ReportEditor editor = Editors.getActive();
			if (editor == null) {
				log.error("unexpected error: report editor is not active");
				return null;
			}
			return editor.getReport();
		} catch (Exception e) {
			log.error("failed to get report from editor", e);
			return null;
		}
	}

	private class ExportAction extends Action {

		private final String CALL_HOOK = "//{{set_data_call}}";

		public ExportAction() {
			setImageDescriptor(ImageType.EXPORT_ICON.getDescriptor());
			setToolTipText("Export report");
		}

		@Override
		public void run() {
			Report report = getReport();
			if (report == null)
				return;
			File dir = FileChooser.forExport(FileChooser.DIRECTORY_DIALOG);
			if (dir == null)
				return;
			File htmlDir = HtmlFolder.getRoot();
			if (htmlDir == null)
				return;
			tryExport(report, dir, htmlDir);
		}

		private void tryExport(Report report, File targetDir, File htmlFolder) {
			try {
				copyLibs(targetDir, htmlFolder);
				String json = new Gson().toJson(report);
				String call = "$(window).load( function() { setData(" + json
						+ ")});";
				File template = HtmlFolder.getFile(HtmlView.REPORT_VIEW
						.getResource());
				List<String> templateLines = Files.readAllLines(
						template.toPath(), Charset.forName("utf-8"));
				List<String> reportLines = new ArrayList<>();
				for (String line : templateLines) {
					String reportLine = line;
					if (line.contains("../libs/"))
						reportLine = line.replace("../libs/", "libs/");
					if (line.contains(CALL_HOOK))
						reportLine = line.replace(CALL_HOOK, call);
					reportLines.add(reportLine);
				}
				writeReport(report.getTitle(), reportLines, targetDir);
			} catch (Exception e) {
				log.error("failed to export report", e);
			}
		}

		private void copyLibs(File targetDir, File htmlFolder)
				throws IOException {
			File libDir = new File(htmlFolder, "libs");
			if (!libDir.exists())
				return;
			File targetLibDir = new File(targetDir, "libs");
			if (!targetLibDir.exists())
				targetLibDir.mkdirs();
			FileUtils.copyDirectory(libDir, targetLibDir);
		}

		private void writeReport(String title, List<String> lines,
				File targetDir) throws IOException {
			String fileName = null;
			if (title == null)
				fileName = "report.html";
			else
				fileName = title.replaceAll("\\W+", "_") + ".html";
			File file = new File(targetDir, fileName);
			Files.write(file.toPath(), lines, Charset.forName("utf-8"));
		}
	}

}
