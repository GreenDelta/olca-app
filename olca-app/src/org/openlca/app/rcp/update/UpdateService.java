package org.openlca.app.rcp.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.core.Response.Status.Family;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;

public class UpdateService {

	private static final Logger log = LoggerFactory
			.getLogger(UpdateService.class);

	public void downloadToFileWithProgress(String url, final File target)
			throws Exception {
		Client c = ApacheHttpClient.create();
		WebResource r2 = c.resource(url);
		final ClientResponse response = r2.get(ClientResponse.class);
		if (response.getClientResponseStatus().getFamily() != Family.SUCCESSFUL) {
			throw new RuntimeException("Download failed: "
					+ response.getClientResponseStatus());
		}

		Job downloadJob = new Job("Downloading " + r2.getURI().getPath()) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					int length = -1;
					if (response.getLength() > 0) {
						length = response.getLength();
					}
					monitor.beginTask("Downloading...", length > 0 ? length
							: IProgressMonitor.UNKNOWN);

					log.debug("Downloading {} mb",
							(length > 0 ? response.getLength() / 1024 / 1024
									: "unknown amount of"));

					download(target, response, monitor);

				} catch (Exception e) {
					log.error("Downloading update failed.", e);
					return Status.CANCEL_STATUS;
				}

				return Status.OK_STATUS;
			}

			protected void download(final File target,
					final ClientResponse response,
					final IProgressMonitor monitor) throws IOException {
				FileUtils.copy(new InputSupplier<InputStream>() {
					@Override
					public InputStream getInput() throws IOException {
						return response.getEntityInputStream();
					}
				}, new OutputSupplier<OutputStream>() {
					@Override
					public OutputStream getOutput() throws IOException {
						return new FileOutputStream(target);
					}
				}, new WorkCallback() {

					private int maxTillNow = 0;
					private int sum = 0;

					@Override
					public void report(int partDone) {
						sum += partDone;
						int newMaxCand = Math.max(sum / (1024 * 1024),
								maxTillNow);
						if (newMaxCand > maxTillNow) {
							log.info("Downloaded {} mb", newMaxCand);
							monitor.worked((newMaxCand - maxTillNow) * 1024 * 1024);
							maxTillNow = newMaxCand;
						}
					}

					@Override
					public void increaseTotal(int newTotal) {
					}
				});
			}
		};
		downloadJob.schedule();
		try {
			while (downloadJob.getResult() == null) {
				Thread.sleep(500);

				log.trace("Checking download job status");
			}
		} catch (InterruptedException ie) {
			log.info("Downlaod supervisor interrupted, aborting.");
		}
		if (!Status.OK_STATUS.equals(downloadJob.getResult())) {
			throw new RuntimeException("Download failed");
		}
	}
}
