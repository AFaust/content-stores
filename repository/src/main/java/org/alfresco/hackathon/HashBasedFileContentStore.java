package org.alfresco.hackathon;

import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import org.alfresco.repo.content.AbstractContentStore;
import org.alfresco.repo.content.ContentContext;
import org.alfresco.repo.content.ContentStoreCreatedEvent;
import org.alfresco.repo.content.EmptyContentReader;
import org.alfresco.repo.content.UnsupportedContentUrlException;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.Deleter;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Implements a FileContentStore that uses a hash based storage structure to provide de-duplication.
 *
 * @author Florian Maul (f.maul@fme.de)
 *
 */
public class HashBasedFileContentStore extends AbstractContentStore implements ApplicationContextAware,
        ApplicationListener<ApplicationEvent>
{

    private static final Logger logger = LoggerFactory.getLogger(HashBasedFileContentStore.class);

    private String rootDirectory;

    private String messageDigestType;

    private boolean deleteEmptyDirs = true;

    private final boolean readOnly = false;

    private ApplicationContext applicationContext;

    public void setRootDirectory(final String rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    public void setMessageDigestType(final String messageDigestType)
    {
        this.messageDigestType = messageDigestType;
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     * Simple constructor
     */
    public HashBasedFileContentStore()
    {
        // NO-OP
    }

    /**
     * Simple constructor (for bad-practice constructor-arg Spring config)
     *
     * @param rootDirectory
     *            the root to store files under
     */
    public HashBasedFileContentStore(final String rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    /**
     * Creates a file from the given relative URL.
     *
     * @param contentUrl
     *            the content URL including the protocol prefix
     * @return Returns a file representing the URL - the file may or may not exist
     * @throws UnsupportedContentUrlException
     *             if the URL is invalid and doesn't support the {@link FileContentStore#STORE_PROTOCOL correct protocol}
     *
     * @see #checkUrl(String)
     */
    // copied from package-protected FileContentStore#makeFile(String) (package-protected is *evil*)
    protected File makeFile(final String contentUrl)
    {
        // take just the part after the protocol
        final Pair<String, String> urlParts = super.getContentUrlParts(contentUrl);
        final String protocol = urlParts.getFirst();
        final String relativePath = urlParts.getSecond();
        // Check the protocol
        if (!protocol.equals(FileContentStore.STORE_PROTOCOL))
        {
            throw new UnsupportedContentUrlException(this, contentUrl);
        }
        // get the file
        final File file = new File(this.rootDirectory, relativePath);
        // done
        return file;
    }

    @Override
    public ContentReader getReader(final String contentUrl)
    {

        try
        {
            final File file = this.makeFile(contentUrl);
            ContentReader reader = null;
            if (file.exists())
            {
                final FileContentReader fileContentReader = new FileContentReader(file, contentUrl);
                reader = fileContentReader;
            }
            else
            {
                reader = new EmptyContentReader(contentUrl);
            }

            // done
            logger.debug("Created content reader: \n   url: {}\n   file: {}\n   reader: {}", new Object[] { contentUrl, file, reader });

            return reader;
        }
        catch (final UnsupportedContentUrlException e)
        {
            // This can go out directly
            throw e;
        }
        catch (final Throwable e)
        {
            throw new ContentIOException("Failed to get reader for URL: " + contentUrl, e);
        }

    }

    @Override
    public boolean isWriteSupported()
    {
        return true;
    }

    @Override
    public ContentWriter getWriter(final ContentContext context)
    {
        return super.getWriter(context);
    }

    @Override
    protected ContentWriter getWriterInternal(final ContentReader existingContentReader, final String newContentUrl)
    {

        // Test of possible wrong input values
        if (newContentUrl != null)
        {
            logger.debug("NewContentUrl ({}) is not NULL!", newContentUrl);
        }

        if (existingContentReader != null)
        {
            logger.debug("existingContentReader ({0}) is not NULL!", existingContentReader);
        }

        return new HashBasedFileContentWriter(this, existingContentReader, this.messageDigestType);
    }

    /*
     * Implementation copied from the FileContentStore
     *
     * @see org.alfresco.repo.content.AbstractContentStore#delete(java.lang.String)
     */
    @Override
    public boolean delete(final String contentUrl)
    {
        if (this.readOnly)
        {
            throw new UnsupportedOperationException("This store is currently read-only: " + this);
        }
        // ignore files that don't exist
        final File file = this.makeFile(contentUrl);
        boolean deleted = false;
        if (!file.exists())
        {
            deleted = true;
        }
        else
        {
            deleted = file.delete();
        }

        // Delete empty parents regardless of whether the file was ignore above.
        if (this.deleteEmptyDirs && deleted)
        {
            Deleter.deleteEmptyParents(file, this.getRootLocation());
        }

        // done
        logger.debug("Delete content directly: \n   store: {}\n   url: {}", this, contentUrl);

        return deleted;
    }

    protected String createContentUrl(final String checksum)
    {
        ParameterCheck.mandatoryString("checksum", checksum);

        if(checksum.length() < 6)
        {
            throw new IllegalArgumentException("Checksum is too short - needs to be at least 6 characters");
        }

        // use the first 6 characters (3 blocks of one byte each if checksum is hex-based) for the base folder structure
        return MessageFormat.format("{4}://{0}/{1}/{2}/{3}.bin", StringUtils.substring(checksum, 0, 2),
                StringUtils.substring(checksum, 2, 4), StringUtils.substring(checksum, 4, 6), checksum, FileContentStore.STORE_PROTOCOL);
    }

    /**
     * Publishes an event to the application context that will notify any interested parties of the existence of this content store.
     *
     * @param context
     *            the application context
     * @param extendedEventParams
     */
    private void publishEvent(final ApplicationContext context, final Map<String, Serializable> extendedEventParams)
    {
        context.publishEvent(new ContentStoreCreatedEvent(this, extendedEventParams));
    }

    public void onApplicationEvent(final ApplicationEvent event)
    {
        // Once the context has been refreshed, we tell other interested beans about the existence of this content store
        // (e.g. for monitoring purposes)
        if (event instanceof ContextRefreshedEvent && event.getSource() == this.applicationContext)
        {
            this.publishEvent(((ContextRefreshedEvent) event).getApplicationContext(), Collections.<String, Serializable> emptyMap());
        }
    }

    public void setDeleteEmptyDirs(final boolean deleteEmptyDirs)
    {
        this.deleteEmptyDirs = deleteEmptyDirs;
    }
}
