package org.alfresco.hackathon.stores.webscript;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Check web-script
 * 
 * @author Andrei T. (andrei.temirbulatov@westernacher.com)
 */
public class CheckNode extends DeclarativeWebScript {

	protected final Logger LOGGER = Logger.getLogger(this.getClass());

	public static final String HACKATHON_NAMESPACE = "http://hackathon2013.alfresco.org/model/fingerprint/1.0";

	public static final QName PROP_FINGERPRINTS = QName.createQName(
			HACKATHON_NAMESPACE, "fingerprints");

	private static final String REQUEST_PARAM_STORETYPE = "storeType";
	private static final String REQUEST_PARAM_STOREID = "storeId";
	private static final String REQUEST_PARAM_ID = "id";

	private static final String REQUEST_PARAM_THUMB = "thumbnailName";
	// property //optional -> cm:content
	private static final String REQUEST_PARAM_PROPERTY = "property";
	// fileName // optional
	private static final String REQUEST_PARAM_FILENAME = "fileName";

	private static final String FIELD_NODEREF = "nodeRef";
	private static final String FIELD_LAST_MODIFIED = "lastModified";
	private static final String FIELD_HASH_VALUES = "hashValues";
	private static final String FIELD_MIME = "mimeType";
	private static final String FIELD_ENCODING = "encoding";
	private static final String FIELD_THUMBNAIL = "thumbnailName";

	private ServiceRegistry services;

	@SuppressWarnings("unchecked")
	@Override
	protected final Map<String, Object> executeImpl(WebScriptRequest request,
			Status status) {

		Map<String, Object> result = new HashMap<String, Object>();

		// node reference
		NodeRef node = getRequestNodeRef(request);

		// checks permissions
		verifyPermissions(node);

		String property = getParameter(request, REQUEST_PARAM_PROPERTY, false);
		QName propertyName = null;
		if (StringUtils.isBlank(property)) {
			propertyName = ContentModel.PROP_CONTENT;
		} else {
			propertyName = QName.createQName(HACKATHON_NAMESPACE, property);
			// QName.createQName(prefix, localName,
			// services.getNamespaceService());
		}

		// optional
		NodeRef thumbNode = null;
		String thumbName = getParameter(request, REQUEST_PARAM_THUMB, false);
		// gets thumbnail
		if (!StringUtils.isBlank(thumbName)) {
			thumbNode = services.getThumbnailService().getThumbnailByName(node,
					propertyName, thumbName);
			if (null == thumbNode) {
				throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND,
						"Thumbnail is missing: " + thumbName);
			} else {
				node = thumbNode;
			}

		} else {
			// tries to find first available thumb
			thumbNode = services.getThumbnailService().getThumbnailByName(node,
					propertyName, "doclib");
			if (null != thumbNode) {
				thumbName = (String) services.getNodeService().getProperty(
						thumbNode, ContentModel.PROP_NAME);
			}
		}

		ContentReader reader = services.getContentService().getReader(node,
				propertyName);
		String encoding = reader.getEncoding();

		String mimeType = null;
		String fileName = getParameter(request, REQUEST_PARAM_FILENAME, false);
		if (StringUtils.isBlank(fileName)) {

			if (reader != null) {
				mimeType = reader.getMimetype();
			}
		} else {
			// guessing mime type
			mimeType = services.getMimetypeService().guessMimetype(fileName);
		}

		Date nodeDate = (Date) services.getNodeService().getProperty(node,
				ContentModel.PROP_MODIFIED);
		List<String> hashValues = (List<String>) services.getNodeService()
				.getProperty(node, ContentModel.PROP_MODIFIED);

		result.put(FIELD_NODEREF, node.toString());
		result.put(FIELD_LAST_MODIFIED, ISO8601DateFormat.format(nodeDate));
		result.put(FIELD_MIME, mimeType);
		result.put(FIELD_ENCODING, encoding);
		result.put(FIELD_HASH_VALUES, hashValues);

		result.put(FIELD_THUMBNAIL, thumbName);

		return result;
	}

	/**
	 * Verifies permissions on the node
	 * 
	 * @param nodeRef
	 *            the node reference
	 */
	protected void verifyPermissions(NodeRef nodeRef) {

		AccessStatus access = services.getPermissionService()
				.hasReadPermission(nodeRef);

		if (access == null || AccessStatus.DENIED == access) {
			throw new WebScriptException(HttpServletResponse.SC_FORBIDDEN,
					"User don't have access to request node");
		}
	}

	/**
	 * Gets mandatory node reference value from request url or parameter.
	 * 
	 * @param request
	 *            request
	 * @return node reference
	 */
	protected NodeRef getRequestNodeRef(WebScriptRequest request) {

		StringBuilder nodeId = new StringBuilder();

		nodeId.append(getParameter(request, REQUEST_PARAM_STORETYPE, true));
		nodeId.append("://");
		nodeId.append(getParameter(request, REQUEST_PARAM_STOREID, true));
		nodeId.append("/");
		nodeId.append(getParameter(request, REQUEST_PARAM_ID, true));

		return getNodeRefForStore(nodeId.toString());
	}

	/**
	 * Returns node ref for given nodeId
	 * 
	 * @param nodeId
	 *            Node ID
	 * @return node ref for given nodeId
	 */
	protected NodeRef getNodeRefForStore(String nodeId) {

		int statusCode = HttpServletResponse.SC_NOT_FOUND;
		boolean throwError = false;

		String errorMessage = "Node with ID: " + nodeId
				+ " doesn't exist in the standard spaces stores";
		NodeRef result = null;
		try {
			if (StringUtils.isEmpty(nodeId)) {
				throwError = true;
			} else {
				result = new NodeRef(nodeId);
				if (!services.getNodeService().exists(result)) {
					throwError = true;
					// statusCode = HttpServletResponse.SC_GONE;
				}
			}
		} catch (Throwable t) {
			LOGGER.error(errorMessage, t);
			throwError = true;
		}
		if (throwError) {
			throw new WebScriptException(statusCode, errorMessage);
		}

		return result;
	}

	/**
	 * Returns request parameter for given name
	 * 
	 * @param request
	 *            Request
	 * @param parameterName
	 *            Parameter name
	 * @return Request value as string
	 */
	protected static String getParameterValue(WebScriptRequest request,
			String parameterName) {
		String value = request.getParameter(parameterName);
		if (StringUtils.isEmpty(value)
				&& request.getServiceMatch().getTemplateVars()
						.containsKey(parameterName)) {
			value = request.getServiceMatch().getTemplateVars()
					.get(parameterName);
		}
		return value;
	}

	/**
	 * Returns request parameter for given name
	 * 
	 * @param request
	 *            Request
	 * @param parameterName
	 *            Parameter name
	 * @param mandatory
	 *            True if mandatory (if this parameter is true and request
	 *            parameter is not specified,<br>
	 *            a WebScriptExcpetion with BAD_REQUEST as status will be
	 *            thrown)
	 * @return Request value as string
	 */
	protected final String getParameter(WebScriptRequest request,
			String parameterName, boolean mandatory) {
		String value = getParameterValue(request, parameterName);
		if (mandatory) {
			checkMandatoryParameter(parameterName, value);
		}
		return value;
	}

	/**
	 * Checks parameter for given value with mandatory check and BASE64 decoding
	 * 
	 * @param paramerName
	 *            Parameter name
	 * @param value
	 *            Value to parse
	 * @return Value as decoded string
	 * @throws WebScriptException
	 *             thrown if mandatory parameter is empty
	 */
	private void checkMandatoryParameter(String paramerName, String value) {
		boolean empty = StringUtils.isEmpty(value);
		if (empty) {
			LOGGER.error("Missing mandatory parameter: " + paramerName);
			throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST,
					paramerName);
		}
	}

	/**
	 * @param services
	 *            the services to set
	 */
	public void setServices(ServiceRegistry services) {
		this.services = services;
	}
}
