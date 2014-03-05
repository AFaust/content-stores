/**
 *
 */
package org.alfresco.hackathon.content.stores.share.web.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.util.ParameterCheck;
import org.alfresco.util.TempFileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.ServletUtil;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.support.AlfrescoUserFactory;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorContext;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.HttpMethod;
import org.springframework.extensions.webscripts.connector.Response;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletResponse;
import org.springframework.util.FileCopyUtils;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class CachingContentStream extends AbstractWebScript implements InitializingBean
{

    private static final String HEADER_CONTENT_RANGE = "Content-Range";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * format definied by RFC 822, see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
     */
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);

    protected static class StreamParams
    {
        private final String storeType;
        private final String storeIdentifier;
        private final String id;

        private final Map<String, String> digests = new HashMap<String, String>();
        private String modifiedDateAsIso;

        private String extensionPath;

        private String property;
        private String thumbnailName;

        private String eTag;

        private String mimetype;
        private String encoding;

        private boolean attachment;
        private String fileName;

        protected StreamParams(final String storeType, final String storeIdentifier, final String id)
        {
            ParameterCheck.mandatory("storeType", storeType);
            ParameterCheck.mandatory("storeIdentifier", storeIdentifier);
            ParameterCheck.mandatory("id", id);
            this.storeType = storeType;
            this.storeIdentifier = storeIdentifier;
            this.id = id;
        }

        /**
         * @return the property
         */
        public final String getProperty()
        {
            return this.property;
        }

        /**
         * @param property
         *            the property to set
         */
        public final void setProperty(final String property)
        {
            this.property = property;
        }

        /**
         * @return the thumbnailName
         */
        public final String getThumbnailName()
        {
            return this.thumbnailName;
        }

        /**
         * @param thumbnailName
         *            the thumbnailName to set
         */
        public final void setThumbnailName(final String thumbnailName)
        {
            this.thumbnailName = thumbnailName;
        }

        /**
         * @return the storeType
         */
        public final String getStoreType()
        {
            return this.storeType;
        }

        /**
         * @return the storeIdentifier
         */
        public final String getStoreIdentifier()
        {
            return this.storeIdentifier;
        }

        /**
         * @return the id
         */
        public final String getId()
        {
            return this.id;
        }

        /**
         * @return the extensionPath
         */
        public final String getExtensionPath()
        {
            return this.extensionPath;
        }

        /**
         * @param extensionPath
         *            the extensionPath to set
         */
        public final void setExtensionPath(final String extensionPath)
        {
            this.extensionPath = extensionPath;
        }

        /**
         * @return the digests
         */
        public final Map<String, String> getDigests()
        {
            return new HashMap<String, String>(this.digests);
        }

        /**
         * @param digestType
         *            the digestType
         * @param digestValue
         *            the digestValue
         */
        public final void addDigest(final String digestType, final String digestValue)
        {
            ParameterCheck.mandatoryString("digestType", digestType);
            ParameterCheck.mandatoryString("digestValue", digestValue);
            this.digests.put(digestType, digestValue);
        }

        /**
         * @return the modifiedDateAsIso
         */
        public final String getModifiedDateAsIso()
        {
            return this.modifiedDateAsIso;
        }

        /**
         * @param modifiedDateAsIso
         *            the modifiedDateAsIso to set
         */
        public final void setModifiedDateAsIso(final String modifiedDateAsIso)
        {
            this.modifiedDateAsIso = modifiedDateAsIso;
        }

        /**
         * @return the eTag
         */
        public final String getETag()
        {
            return this.eTag;
        }

        /**
         * @param eTag
         *            the eTag to set
         */
        public final void setETag(final String eTag)
        {
            this.eTag = eTag;
        }

        /**
         * @return the mimetype
         */
        public final String getMimetype()
        {
            return this.mimetype;
        }

        /**
         * @param mimetype
         *            the mimetype to set
         */
        public final void setMimetype(final String mimetype)
        {
            this.mimetype = mimetype;
        }

        /**
         * @return the encoding
         */
        public final String getEncoding()
        {
            return this.encoding;
        }

        /**
         * @param encoding
         *            the encoding to set
         */
        public final void setEncoding(final String encoding)
        {
            this.encoding = encoding;
        }

        /**
         * @return the attachment
         */
        public final boolean isAttachment()
        {
            return this.attachment;
        }

        /**
         * @param attachment
         *            the attachment to set
         */
        public final void setAttachment(final boolean attachment)
        {
            this.attachment = attachment;
        }

        /**
         * @return the fileName
         */
        public final String getFileName()
        {
            return this.fileName;
        }

        /**
         * @param fileName
         *            the fileName to set
         */
        public final void setFileName(final String fileName)
        {
            this.fileName = fileName;
        }

    }

    protected ConnectorService connectorService;

    protected File localCacheDirectory;

    protected long maxCacheQuota;

    protected long maxCacheTtl;

    public void afterPropertiesSet() throws Exception
    {
        // Set up a temporary store
        this.localCacheDirectory = TempFileProvider.getTempDir();
    }

    /**
     * @return the maxCacheQuota
     */
    public final long getMaxCacheQuota()
    {
        return this.maxCacheQuota;
    }

    /**
     * @param connectorService
     *            the connectorService to set
     */
    public final void setConnectorService(final ConnectorService connectorService)
    {
        this.connectorService = connectorService;
    }

    /**
     * @param maxCacheTtl
     *            the maxCacheTtl to set
     */
    public final void setMaxCacheTtl(final long maxCacheTtl)
    {
        this.maxCacheTtl = maxCacheTtl;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(final WebScriptRequest req, final WebScriptResponse res) throws IOException
    {
        final Status status = new Status();

        final StreamParams streamParameters = this.readStreamParameters(req);

        this.checkRepository(req, res, status, streamParameters);

        if (status.getRedirect())
        {
            if (status.getCode() == Status.STATUS_NOT_FOUND && streamParameters.getThumbnailName() != null)
            {
                // the thumbnail may not yet exist
                this.callThroughThumbnail(req, res, streamParameters);
            }
            else
            {
                this.sendStatus(req, res, status);
            }
        }
        else
        {

            final String modifiedSinceStr = req.getHeader("If-Modified-Since");
            if (modifiedSinceStr != null && streamParameters.getModifiedDateAsIso() != null)
            {
                long modifiedSince = 0;
                try
                {
                    modifiedSince = dateFormat.parse(modifiedSinceStr).getTime();
                }
                catch (final Throwable e)
                {
                    modifiedSince = 0;
                }

                if (modifiedSince > 0L)
                {
                    final Date modified = ISO8601DateFormat.parse(streamParameters.getModifiedDateAsIso());
                    // round the date to the ignore millisecond value which is not supplied by header
                    final long modDate = (modified.getTime() / 1000L) * 1000L;
                    if (modDate <= modifiedSince)
                    {
                        res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        return;
                    }
                }
            }

            File cachedFile = this.getCachedFile(streamParameters);
            if (cachedFile == null)
            {
                cachedFile = this.cacheFile(streamParameters, status);
            }

            if (status.getRedirect())
            {
                this.sendStatus(req, res, status);
            }
            else
            {
                this.streamCachedFileImpl(req, res, streamParameters, cachedFile);
            }
        }
    }

    protected void sendStatus(final WebScriptRequest req, final WebScriptResponse res, final Status status) throws IOException
    {
        final String format = req.getFormat();
        final Cache cache = new Cache(this.getDescription().getRequiredCache());
        final Map<String, Object> model = new HashMap<String, Object>();
        model.put("cache", cache);
        model.put("status", status);

        final Map<String, Object> templateModel = this.createTemplateParameters(req, res, model);
        this.sendStatus(req, res, status, cache, format, templateModel);
    }

    protected StreamParams readStreamParameters(final WebScriptRequest req)
    {
        final Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        final String storeType = templateVars.get("storeType");
        final String storeId = templateVars.get("storeId");
        final String id = templateVars.get("id");

        ParameterCheck.mandatoryString("storeType", storeType);
        ParameterCheck.mandatoryString("storeId", storeId);
        ParameterCheck.mandatoryString("id", id);

        final StreamParams params = new StreamParams(storeType, storeId, id);

        if (templateVars.containsKey("thumbnailName"))
        {
            final String thumbnailName = templateVars.get("thumbnailName");
            ParameterCheck.mandatoryString("thumbnailName", thumbnailName);
            params.setThumbnailName(thumbnailName);
        }

        if (templateVars.containsKey("fileName"))
        {
            params.setFileName(templateVars.get("fileName"));
        }

        params.setExtensionPath(req.getExtensionPath());

        final String attachParameter = req.getParameter("attach");
        final String aParameter = req.getParameter("a");
        if ((attachParameter != null && Boolean.parseBoolean(attachParameter)) || (aParameter != null && Boolean.parseBoolean(aParameter)))
        {
            params.setAttachment(true);
        }

        return params;
    }

    protected void checkRepository(final WebScriptRequest req, final WebScriptResponse res, final Status status, final StreamParams params)
            throws IOException
    {
        try
        {
            final Response response = this.executeRepositoryRequest(params);

            if (response.getStatus().getCode() == 200)
            {
                final JSONTokener jsonTokener = new JSONTokener(response.getResponse());
                final JSONObject jsonObject = new JSONObject(jsonTokener);

                final String mimetype = jsonObject.getString("mimetype");
                params.setMimetype(mimetype);

                final String encoding = jsonObject.getString("encoding");
                params.setEncoding(encoding);

                final String modified = jsonObject.getString("modified");
                params.setModifiedDateAsIso(modified);

                final JSONArray digests = jsonObject.getJSONArray("fingerprints");
                for (int i = 0, max = digests.length(); i < max; i++)
                {
                    final JSONObject digest = digests.getJSONObject(i);
                    params.addDigest(digest.getString("digestType"), digest.getString("digestValue"));
                }
            }
            else
            {
                status.setCode(response.getStatus().getCode(), response.getStatus().getMessage());
                status.setRedirect(true);
            }
        }
        catch (final Exception ex)
        {
            status.setCode(Status.STATUS_INTERNAL_SERVER_ERROR, ex.getMessage());
            status.setException(ex);
            status.setRedirect(true);
        }
    }

    protected Response executeRepositoryRequest(final StreamParams params) throws ConnectorServiceException, IOException
    {
        final RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();
        final String currentUserId = requestContext.getUserId();
        final HttpSession currentSession = ServletUtil.getSession(true);
        final Connector connector = this.connectorService.getConnector(AlfrescoUserFactory.ALFRESCO_ENDPOINT_ID, currentUserId,
                currentSession);

        final StringBuilder uri = new StringBuilder(MessageFormat.format("/api/node/{0}/{1}/{2}", URLEncoder.encode(params.getStoreType()),
                URLEncoder.encode(params.getStoreIdentifier()), URLEncoder.encode(params.getId())));

        if (params.getProperty() != null)
        {
            uri.append("/").append(URLEncoder.encode(params.getProperty()));
        }
        else if (params.getThumbnailName() != null)
        {
            uri.append("/").append(URLEncoder.encode(params.getThumbnailName()));
        }

        uri.append("/fingerprints?noCache=");
        uri.append(String.valueOf(System.currentTimeMillis()));

        final Response response = connector.call(uri.toString());
        return response;
    }

    protected File getCachedFile(final StreamParams streamParameters)
    {
        File result = null;
        for (final Entry<String, String> digest : streamParameters.getDigests().entrySet())
        {
            final File expectedCachedFile = new File(MessageFormat.format("{0}/{1}/{2}.bin.cached",
                    this.localCacheDirectory.getAbsolutePath(), digest.getKey(), digest.getValue()));
            if (expectedCachedFile.exists())
            {
                result = expectedCachedFile;
                break;
            }
        }

        if (result == null)
        {
            // check by NodeRef but only if created after last modification
            final File expectedIdentity = new File(this.localCacheDirectory, MessageFormat.format("{0}+{1}+{2}+{3}.bin.cached",
                    streamParameters.getStoreType(), streamParameters.getStoreIdentifier(), streamParameters.getId(),
                    streamParameters.getProperty()));
            if (expectedIdentity.exists())
            {
                final long lastModified = expectedIdentity.lastModified();
                final String modifiedDateAsIso = streamParameters.getModifiedDateAsIso();

                if (modifiedDateAsIso != null && ISO8601DateFormat.parse(modifiedDateAsIso).getTime() < lastModified)
                {
                    // the cached file is not outdated
                    result = expectedIdentity;
                }
            }
        }

        if (result != null && this.maxCacheTtl > 0l)
        {
            final long lastModified = result.lastModified();
            if (System.currentTimeMillis() - lastModified > this.maxCacheTtl)
            {
                result.delete();
                result = null;
            }
        }

        return result;
    }

    protected File cacheFile(final StreamParams streamParameters, final Status status)
    {
        File cacheFile = null;
        try
        {
            final RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();
            final String currentUserId = requestContext.getUserId();
            final HttpSession currentSession = ServletUtil.getSession(true);
            final Connector connector = this.connectorService.getConnector(AlfrescoUserFactory.ALFRESCO_ENDPOINT_ID, currentUserId,
                    currentSession);

            final String property = streamParameters.getProperty() != null ? streamParameters.getProperty() : "";
            final StringBuilder uri = new StringBuilder(MessageFormat.format("/api/node/{0}/{1}/{2}/content{3}",
                    URLEncoder.encode(streamParameters.getStoreType()), URLEncoder.encode(streamParameters.getStoreIdentifier()),
                    URLEncoder.encode(streamParameters.getId()), URLEncoder.encode(property)));

            if (streamParameters.getThumbnailName() != null)
            {
                uri.append("/thumbnails/").append(URLEncoder.encode(streamParameters.getThumbnailName()));
                uri.append("?c=force");
            }
            else
            {
                uri.append("?a=false");
            }

            final Map<String, String> digests = streamParameters.getDigests();

            if (digests.isEmpty())
            {
                cacheFile = new File(this.localCacheDirectory, MessageFormat.format("{0}+{1}+{2}+{3}.bin.cached",
                        streamParameters.getStoreType(), streamParameters.getStoreIdentifier(), streamParameters.getId(),
                        streamParameters.getProperty()));
            }
            else
            {
                // TODO: symbolic links / in-memory mapping of multiple digests => file
                final Entry<String, String> digest = digests.entrySet().iterator().next();
                cacheFile = new File(MessageFormat.format("{0}/{1}/{2}.bin.cached", this.localCacheDirectory.getAbsolutePath(),
                        digest.getKey(), digest.getValue()));
            }

            if (!cacheFile.exists())
            {
                final File parent = cacheFile.getParentFile();
                if (!parent.exists())
                {
                    parent.mkdirs();
                }

                cacheFile.createNewFile();
                cacheFile.deleteOnExit();
            }

            // build a connector context, stores information about how we will drive the remote client
            // ensure we don't proxy over any browser to web-tier Authorization headers over to the endpoint
            final Map<String, String> headers = new HashMap<String, String>(1, 1.0f);
            headers.put("Authorization", null);
            final ConnectorContext context = new ConnectorContext(HttpMethod.GET, null, headers);
            context.setExceptionOnError(true);

            final FileOutputStream fout = new FileOutputStream(cacheFile);
            final Response response = connector.call(uri.toString(), context, null, fout);
            if (response.getStatus().getCode() != 200)
            {
                cacheFile.delete();
                status.setCode(response.getStatus().getCode(), response.getStatus().getMessage());
                status.setRedirect(true);
            }

            // TODO: quota handling
        }
        catch (final Exception ex)
        {
            if (cacheFile != null)
            {
                cacheFile.delete();
                cacheFile = null;
            }

            status.setCode(Status.STATUS_INTERNAL_SERVER_ERROR, ex.getMessage());
            status.setException(ex);
            status.setRedirect(true);
        }

        return cacheFile;
    }

    protected void streamCachedFileImpl(final WebScriptRequest req, final WebScriptResponse res, final StreamParams streamParameters,
            final File cachedFile) throws IOException
    {

        if (streamParameters.isAttachment())
        {
            String headerValue = "attachment";
            final String attachFileName = streamParameters.getFileName() != null ? streamParameters.getFileName() : streamParameters
                    .getExtensionPath();
            if (attachFileName != null && attachFileName.length() > 0)
            {
                headerValue += "; filename*=UTF-8''" + URLEncoder.encode(attachFileName) + "; filename=\"" + attachFileName + "\"";
            }

            // set header based on filename - will force a Save As from the browse if it doesn't recognize it
            // this is better than the default response of the browser trying to display the contents
            res.setHeader("Content-Disposition", headerValue);
        }

        res.setContentType(streamParameters.getMimetype());
        res.setContentEncoding(streamParameters.getEncoding());

        final long size = cachedFile.length();
        res.setHeader(HEADER_CONTENT_RANGE, "bytes 0-" + Long.toString(size - 1L) + "/" + Long.toString(size));
        res.setHeader(HEADER_CONTENT_LENGTH, Long.toString(size));

        FileCopyUtils.copy(new FileInputStream(cachedFile), res.getOutputStream());
    }

    protected void callThroughThumbnail(final WebScriptRequest req, final WebScriptResponse res, final StreamParams streamParameters)
    {
        try
        {
            final RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();
            final String currentUserId = requestContext.getUserId();
            final HttpSession currentSession = ServletUtil.getSession(true);
            final Connector connector = this.connectorService.getConnector(AlfrescoUserFactory.ALFRESCO_ENDPOINT_ID, currentUserId,
                    currentSession);

            final StringBuilder uri = new StringBuilder(MessageFormat.format("/api/node/{0}/{1}/{2}/content/thumbnails/{3}",
                    streamParameters.getStoreType(), streamParameters.getStoreIdentifier(), streamParameters.getId(),
                    streamParameters.getThumbnailName()));

            if (streamParameters.getFileName() != null)
            {
                uri.append("/").append(URLEncoder.encode(streamParameters.getFileName()));
            }

            final String c = req.getParameter("c");
            final String ph = req.getParameter("ph");

            if (c != null || ph != null)
            {
                uri.append("?");

                boolean initialParam = true;
                if (c != null)
                {
                    if (!initialParam)
                    {
                        uri.append("&");
                    }
                    uri.append("c=").append(URLEncoder.encode(c));
                    initialParam = false;
                }

                if (ph != null)
                {
                    if (!initialParam)
                    {
                        uri.append("&");
                    }
                    uri.append("ph=").append(URLEncoder.encode(ph));
                    initialParam = false;
                }
            }

            // build a connector context, stores information about how we will drive the remote client
            // ensure we don't proxy over any browser to web-tier Authorization headers over to the endpoint
            final Map<String, String> headers = new HashMap<String, String>(1, 1.0f);
            headers.put("Authorization", null);
            final ConnectorContext context = new ConnectorContext(
                    req instanceof WebScriptServletRequest ? HttpMethod.valueOf(((WebScriptServletRequest) req).getHttpServletRequest()
                            .getMethod().toUpperCase()) : HttpMethod.GET, null, headers);
            context.setExceptionOnError(true);
            context.setContentType(req.getContentType());
            // TODO: handle non-servlet requests / responses
            connector.call(uri.toString(), context,
                    req instanceof WebScriptServletRequest ? ((WebScriptServletRequest) req).getHttpServletRequest() : null,
                    res instanceof WebScriptServletResponse ? ((WebScriptServletResponse) res).getHttpServletResponse() : null);
        }
        catch (final Exception ex)
        {
            // TODO: log
        }
    }
}
