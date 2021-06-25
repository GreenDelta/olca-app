package org.openlca.app.editors.projects.results;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.images.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ReportToolbar extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(new ExportAction());
	}

	private Report getReport() {
		try {
			ReportViewer editor = Editors.getActive();
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
			setImageDescriptor(Icon.EXPORT.descriptor());
			setToolTipText(M.ExportReport);
		}

		@Override
		public void run() {
			Report report = getReport();
			if (report == null)
				return;
			File dir = FileChooser.selectFolder();
			if (dir == null)
				return;
			File htmlDir = HtmlFolder.getDir(RcpActivator.getDefault()
					.getBundle());
			if (htmlDir == null)
				return;
			tryExport(report, dir, htmlDir);
		}

		private void tryExport(Report report, File targetDir, File htmlFolder) {
			try {
				copyLibs(targetDir, htmlFolder);
				String json = new Gson().toJson(report);
				String call = "document.addEventListener(\"DOMContentLoaded\", "
						+ "function() { setData(" + json + "); });";
				File template = HtmlFolder.getFile(
						RcpActivator.getDefault().getBundle(), "report.html");
				StringBuilder text = new StringBuilder();
				Files.readAllLines(template.toPath(), Charset.forName("utf-8"))
						.stream().map(line -> line.contains(CALL_HOOK)
								? line.replace(CALL_HOOK, call)
								: line)
						.forEach(line -> {
							text.append(line);
							text.append('\n');
						});

				String fileName = report.title == null
						? "report.html"
						: report.title.replaceAll("\\W+", "_") + ".html";
				File file = new File(targetDir, fileName);
				Files.write(file.toPath(), text.toString().getBytes("utf-8"));
			} catch (Exception e) {
				log.error("failed to export report", e);
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
				targetLibDir.mkdirs();
			}
			FileUtils.copyDirectory(libDir, targetLibDir);
		}
	}
}
