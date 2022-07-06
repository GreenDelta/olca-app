package org.openlca.app.collaboration.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DownloadJsonInvocation extends Invocation<InputStream, Void> {

	private static final Logger log = LoggerFactory.getLogger(DownloadJsonInvocation.class);
	private final String token;
	private final File toFile;

	DownloadJsonInvocation(String token, File toFile) {
		super(Type.GET, "public/download/json", InputStream.class);
		this.token = token;
		this.toFile = toFile;
	}

	@Override
	protected void checkValidity() {
		Valid.checkNotEmpty(token, "token");
	}

	@Override
	protected String query() {
		return "/" + token;
	}

	@Override
	protected Void process(InputStream response) {
		try {
			Files.copy(response, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return null;
		} catch (IOException e) {
			log.error("Error downloading json", e);
			return null;
		}
	}

}