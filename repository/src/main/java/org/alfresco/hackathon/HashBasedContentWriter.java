package org.alfresco.hackathon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.alfresco.repo.content.AbstractContentWriter;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements a {@link ContentWriter} that stores files based on a hash structure.
 * 
 * @author Florian Maul (f.maul@fme.de)
 */
public class HashBasedContentWriter extends AbstractContentWriter {

	private static final Log Logger = LogFactory.getLog(HashBasedContentWriter.class);

	private long size;

	private String contentUrl;

	private HashBasedFileContentStore store;

	private File tempFile;

	private MessageDigest messageDigest;

	public MessageDigest getMessageDigest() {
		return messageDigest;
	}

	public HashBasedFileContentStore getStore() {
		return store;
	}

	public HashBasedContentWriter(HashBasedFileContentStore contentStore,
			ContentReader existingContentReader) {
		super("dedup://dummy", existingContentReader);
		this.store = contentStore;
		addListener(new HashBasedWriterStreamListener(this));
	}

	@Override
	public ContentData getContentData() {
		ContentData property = new ContentData(getContentUrl(), getMimetype(),
				getSize(), getEncoding());
		return property;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getContentUrl() {
		return contentUrl;
	}

	public void setContentUrl(String contentUrl) {
		this.contentUrl = contentUrl;
	}

	@Override
	protected ContentReader createReader() throws ContentIOException {
		Logger.debug("ContentUrl=" + getContentUrl());
		return store.getReader(getContentUrl());
	}

	@Override
	protected WritableByteChannel getDirectWritableChannel() {
		try {
			tempFile = TempFileProvider.createTempFile("dedup", "tmp");

			messageDigest = MessageDigest.getInstance("SHA-1");
			OutputStream os = new DigestOutputStream(new FileOutputStream(
					tempFile), messageDigest);

			return Channels.newChannel(os);
		} catch (Throwable e) {
			throw new ContentIOException(
					"Exception in getDirectWritableChannel(): " + this, e);
		}
	}

	/**
	 * @return the temp file used for the current write operation
	 */
	public File getTempFile() {
		return tempFile;
	}
}
