package org.alfresco.hackathon;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.coremedia.iso.Hex;

/**
 * Implements a stream listener that copies a new file to the target destination based on the has value that was created from the file.
 * 
 * @author Florian Maul (f.maul@fme.de)
 */
public class HashBasedWriterStreamListener implements ContentStreamListener {
	private static final Log logger = LogFactory
			.getLog(HashBasedWriterStreamListener.class);

	private HashBasedContentWriter writer;

	private HashBasedFileContentStore store;

	/**
	 * Public constructor
	 */
	public HashBasedWriterStreamListener(HashBasedContentWriter writer) {
		this.writer = writer;
		this.store = writer.getStore();
	}

	/**
	 * If content is streamed completely, this method is called
	 */
	public void contentStreamClosed() throws ContentIOException {

		File tempFile = writer.getTempFile();

		MessageDigest messageDigest = writer.getMessageDigest();
		String sha1 = Hex.encodeHex(messageDigest.digest());
		
		if (tempFile != null) {
			
			String contentUrl = store.createContentUrl(tempFile, sha1);
			
			File targetFile = store.makeFile(contentUrl);
			if (!targetFile.exists()) {
				try {
					FileUtils.copyFile(tempFile, targetFile);
				} catch (IOException e) {
					logger.error("Error copying file from temp to content store.");
				}
			}
			
			//now we can set the new important values for db
			writer.setContentUrl(contentUrl);
			writer.setSize(tempFile.length());
			
			FileUtils.deleteQuietly(tempFile);
		}
	}
}
