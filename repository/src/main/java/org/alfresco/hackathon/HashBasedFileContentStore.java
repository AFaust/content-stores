package org.alfresco.hackathon;

import java.io.File;

import org.alfresco.repo.content.AbstractContentStore;
import org.alfresco.repo.content.ContentContext;
import org.alfresco.repo.content.EmptyContentReader;
import org.alfresco.repo.content.UnsupportedContentUrlException;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.Deleter;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements a FileContentStore that uses a hash based storage structure to provide de-duplication.
 * 
 * @author Florian Maul (f.maul@fme.de)
 *
 */
public class HashBasedFileContentStore extends AbstractContentStore {
	
	private static final Log logger = LogFactory.getLog(HashBasedFileContentStore.class);

	private String rootDirectory;
	
	private boolean allowRandomAccess = true;
	
	private boolean deleteEmptyDirs = true;

	private boolean readOnly = false;
	
	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}
	
	//Constructor
	public HashBasedFileContentStore(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

    
    /**
     * Creates a file from the given relative URL.
     * 
     * @param contentUrl    the content URL including the protocol prefix
     * @return              Returns a file representing the URL - the file may or may not
     *                      exist
     * @throws UnsupportedContentUrlException
     *                      if the URL is invalid and doesn't support the
     *                      {@link FileContentStore#STORE_PROTOCOL correct protocol}
     * 
     * @see #checkUrl(String)
     */
    /*package*/ File makeFile(String contentUrl)
    {
        // take just the part after the protocol
        Pair<String, String> urlParts = super.getContentUrlParts(contentUrl);
        String protocol = urlParts.getFirst();
        String relativePath = urlParts.getSecond();
        // Check the protocol
        if (!protocol.equals(FileContentStore.STORE_PROTOCOL))
        {
            throw new UnsupportedContentUrlException(this, contentUrl);
        }
        // get the file
        File file = new File(rootDirectory, relativePath);
        // done
        return file;
    }	
	
	@Override
	public ContentReader getReader(String contentUrl) {
		
        try
        {
            File file = makeFile(contentUrl);
            ContentReader reader = null;
            if (file.exists())
            {
                FileContentReader fileContentReader = new FileContentReader(file, contentUrl);
                //fileContentReader.setAllowRandomAccess(allowRandomAccess);
                reader = fileContentReader;
            }
            else
            {
                reader = new EmptyContentReader(contentUrl);
            }
            
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Created content reader: \n" +
                        "   url: " + contentUrl + "\n" +
                        "   file: " + file + "\n" +
                        "   reader: " + reader);
            }
            return reader;
        }
        catch (UnsupportedContentUrlException e)
        {
            // This can go out directly
            throw e;
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to get reader for URL: " + contentUrl, e);
        }

	}

	@Override
	public boolean isWriteSupported() {
		return true;
	}

	@Override
	public ContentWriter getWriter(ContentContext context) {
		return super.getWriter(context);
	}


	@Override
	protected ContentWriter getWriterInternal(
			ContentReader existingContentReader, String newContentUrl) {
		
		//Test of possible wrong input values
		if (newContentUrl != null) {
	    	logger.debug("NewContentUrl (" + newContentUrl + ") is not NULL!" );
	    }
		
	    if (existingContentReader != null) {
	      logger.debug("existingContentReader (" + existingContentReader.toString() + ") is not NULL!" );
	    }
	    
	    return new HashBasedContentWriter(this, existingContentReader);
	}
	
	/* Implementation copied from the FileContentStore
	 * @see org.alfresco.repo.content.AbstractContentStore#delete(java.lang.String)
	 */
	@Override
	public boolean delete(String contentUrl)
    {
        if (readOnly)
        {
            throw new UnsupportedOperationException("This store is currently read-only: " + this);
        }
        // ignore files that don't exist
        File file = makeFile(contentUrl);
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
        if (deleteEmptyDirs && deleted)
        {
            Deleter.deleteEmptyParents(file, getRootLocation());
        }

        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Delete content directly: \n" +
                    "   store: " + this + "\n" +
                    "   url: " + contentUrl);
        }
        return deleted;
    }

	public String createContentUrl(File tempFile, String checksum) {
		return "store://" + 
				StringUtils.substring(checksum, 0, 2) + "/" + 
				StringUtils.substring(checksum, 2, 4) + "/" + 
				StringUtils.substring(checksum, 4, 6) + "/" + checksum + ".bin";
	}

	public void setDeleteEmptyDirs(boolean deleteEmptyDirs) {
		this.deleteEmptyDirs = deleteEmptyDirs;
	}
}
