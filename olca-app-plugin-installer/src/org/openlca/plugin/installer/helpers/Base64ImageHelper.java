package org.openlca.plugin.installer.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;

public class Base64ImageHelper {

	/**
	 * @param args
	 *            image paths
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please provide image paths as args.");
		} else {

			for (String arg : args) {
				try {
					Base64ImageHelper base64ImageCreator = new Base64ImageHelper();

					String basedImage = base64ImageCreator
							.createBase64EncodedImage(arg);
					System.out.println("" + arg + ": " + basedImage);
				} catch (Exception e) {
					System.err.println("Problem with argument '" + arg + "': "
							+ e.getMessage());
					System.exit(1);
				}
			}

		}
	}

	public String createBase64EncodedImage(final String arg) throws IOException {

		return createBase64EncodedImage(new InputSupplier<InputStream>() {
			@Override
			public InputStream getInput() throws IOException {
				return new FileInputStream(arg);
			}
		});
	}

	public String createBase64EncodedImage(
			InputSupplier<? extends InputStream> stream) throws IOException {
		StringWriter stringWriter = new StringWriter();
		OutputStream os = BaseEncoding.base64().encodingStream(stringWriter);
		ByteStreams.copy(stream, os);
		os.close();
		return stringWriter.toString();
	}

	public InputSupplier<InputStream> getBase64ImageDecoder(final String base64)
			throws IOException {

		return getBase64ImageDecoder(new InputSupplier<Reader>() {
			@Override
			public Reader getInput() throws IOException {
				return new StringReader(base64);
			}
		});
	}

	public InputSupplier<InputStream> getBase64ImageDecoder(
			InputSupplier<? extends Reader> in) throws IOException {
		return BaseEncoding.base64().decodingStream(in);
	}

	public byte[] decodeBase64EncodedImage(String s) throws IOException {
		return ByteStreams.toByteArray(getBase64ImageDecoder(s));
	}

}
