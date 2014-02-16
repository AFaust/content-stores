package org.alfresco.hackathon.content.stores.repo;

import org.alfresco.service.namespace.QName;

/**
 * Plain interface for content stores hackathon project model constants.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ContentStoresModel
{

    static final String NAMESPACE_URI = "http://hackathon2013.alfresco.org/model/fingerprint/1.0";
    static final String NAMESPACE_PREFIX = "hack";

    static final QName DATATYPE_CONTENT_FINGERPRINT = QName.createQName(NAMESPACE_URI, "contentFingerprint");

    static final QName ASPECT_FINGERPRINT_DATA = QName.createQName(NAMESPACE_URI, "fingerprintData");
    static final QName PROP_FINGERPRINTS = QName.createQName(NAMESPACE_URI, "fingerprints");

}
