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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a {@link ContentWriter} that stores files based on a hash structure.
 *
 * @author Florian Maul (f.maul@fme.de)
 */
public class HashBasedFileContentWriter extends AbstractContentWriter
{

    private static final Logger Logger = LoggerFactory.getLogger(HashBasedFileContentWriter.class);

    private long size;

    private String contentUrl;

    private final HashBasedFileContentStore store;

    private File tempFile;

    private final String messageDigestType;

    private MessageDigest messageDigest;

    public MessageDigest getMessageDigest()
    {
        return this.messageDigest;
    }

    public HashBasedFileContentStore getStore()
    {
        return this.store;
    }

    public HashBasedFileContentWriter(final HashBasedFileContentStore contentStore, final ContentReader existingContentReader, final String messageDigestType)
    {
        super("dedup://dummy", existingContentReader);
        this.store = contentStore;
        this.messageDigestType = messageDigestType;
        this.addListener(new HashBasedWriterStreamListener(this));
    }

    @Override
    public ContentData getContentData()
    {
        final ContentData property = new ContentData(this.getContentUrl(), this.getMimetype(), this.getSize(), this.getEncoding());
        return property;
    }

    @Override
    public long getSize()
    {
        if(this.tempFile != null && this.tempFile.exists())
        {
            return this.tempFile.length();
        }
        return this.size;
    }

    // internal for update before tempFile is deleted
    protected void setSize(final long size)
    {
        this.size = size;
    }

    @Override
    public String getContentUrl()
    {
        if(this.contentUrl == null)
        {
            return super.getContentUrl();
        }
        return this.contentUrl;
    }

    // internal for update before tempFile is deleted
    protected void setContentUrl(final String contentUrl)
    {
        this.contentUrl = contentUrl;
    }

    @Override
    protected ContentReader createReader() throws ContentIOException
    {
        Logger.debug("ContentUrl={}", this.getContentUrl());
        return this.store.getReader(this.getContentUrl());
    }

    @Override
    protected WritableByteChannel getDirectWritableChannel()
    {
        try
        {
            this.tempFile = TempFileProvider.createTempFile("dedup", "tmp");

            this.messageDigest = MessageDigest.getInstance(this.messageDigestType);
            final OutputStream os = new DigestOutputStream(new FileOutputStream(this.tempFile), this.messageDigest);

            return Channels.newChannel(os);
        }
        catch (final Throwable e)
        {
            throw new ContentIOException("Exception in getDirectWritableChannel(): " + this, e);
        }
    }

    /**
     * @return the temp file used for the current write operation
     */
    public File getTempFile()
    {
        return this.tempFile;
    }
}
