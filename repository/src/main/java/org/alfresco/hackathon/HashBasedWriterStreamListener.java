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
public class HashBasedWriterStreamListener implements ContentStreamListener
{
    private static final Log logger = LogFactory.getLog(HashBasedWriterStreamListener.class);

    private final HashBasedFileContentWriter writer;

    private final HashBasedFileContentStore store;

    /**
     * Public constructor
     */
    public HashBasedWriterStreamListener(final HashBasedFileContentWriter writer)
    {
        this.writer = writer;
        this.store = writer.getStore();
    }

    /**
     * If content is streamed completely, this method is called
     */
    public void contentStreamClosed() throws ContentIOException
    {

        final File tempFile = this.writer.getTempFile();

        final MessageDigest messageDigest = this.writer.getMessageDigest();
        final String digestHex = Hex.encodeHex(messageDigest.digest());

        if (tempFile != null)
        {
            try
            {
                final String contentUrl = this.store.createContentUrl(digestHex);

                final File targetFile = this.store.makeFile(contentUrl);
                if (!targetFile.exists())
                {
                    try
                    {
                        FileUtils.copyFile(tempFile, targetFile);
                    }
                    catch (final IOException e)
                    {
                        logger.error("Error copying file from temp to content store.");
                    }
                }
                // else: a file with identical content already exists

                // now we can set the new important values for db
                this.writer.setContentUrl(contentUrl);
                this.writer.setSize(tempFile.length());

            }
            finally
            {
                FileUtils.deleteQuietly(tempFile);
            }
        }
    }
}
