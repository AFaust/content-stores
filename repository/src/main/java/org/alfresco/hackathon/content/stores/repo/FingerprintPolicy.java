package org.alfresco.hackathon.content.stores.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentPropertyUpdatePolicy;
import org.alfresco.repo.domain.node.NodePropertyValue;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Policy to generate content fingerprints on content property updates
 *
 */
public class FingerprintPolicy implements OnContentPropertyUpdatePolicy, InitializingBean
{

    // TODO: find a better place for this
    // can't be in ContentFingerprint itself (to avoid issues in SOLR), but is not a part of the policy either
    static
    {
        // this requires at least Alfresco 4.1.1.3
        NodePropertyValue.IMMUTABLE_CLASSES.add(ContentFingerprint.class);

        try
        {
            // unfortunately, Alfresco by default does not offer a proper way to register new datatypes
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final Class<? extends Enum> valueTypeClass = (Class<? extends Enum>) Class.forName(NodePropertyValue.class.getCanonicalName()
                    + "$ValueType");
            @SuppressWarnings("unchecked")
            final Object stringValueType = Enum.valueOf(valueTypeClass, "SERIALIZABLE");

            final Field valueTypesByPropertyTypeField = NodePropertyValue.class.getDeclaredField("valueTypesByPropertyType");
            valueTypesByPropertyTypeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            final Map<QName, Object> valueTypesByPropertyType = (Map<QName, Object>) valueTypesByPropertyTypeField.get(null);
            valueTypesByPropertyType.put(ContentStoresModel.DATATYPE_CONTENT_FINGERPRINT, stringValueType);
        }
        catch (final Throwable e)
        {
            throw new AlfrescoRuntimeException("Failed to register custom datatype", e);
        }
    }

    private static Logger LOGGER = LoggerFactory.getLogger(FingerprintPolicy.class);

    private PolicyComponent policyComponent;

    private ContentService contentService;

    private NodeService nodeService;

    private List<String> digestTypes = Collections.emptyList();

    public void setPolicyComponent(final PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    /**
     * @param contentService
     *            the contentService to set
     */
    public final void setContentService(final ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param nodeService
     *            the nodeService to set
     */
    public final void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setDigestTypes(final String digestTypes)
    {
        if (digestTypes != null && digestTypes.length() != 0)
        {
            this.digestTypes = Arrays.asList(digestTypes.split(","));
        }
        else
        {
            this.digestTypes = Collections.emptyList();
        }
    }

    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "contentService", this.contentService);
        PropertyCheck.mandatory(this, "policyComponent", this.policyComponent);

        this.policyComponent.bindClassBehaviour(OnContentPropertyUpdatePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this,
                "onContentPropertyUpdate", NotificationFrequency.EVERY_EVENT));
    }

    public void onContentPropertyUpdate(final NodeRef nodeRef, final QName propertyQName, final ContentData beforeValue,
            final ContentData afterValue)
    {
        LOGGER.trace("onContentPropertyUpdate: {} for {}", nodeRef, propertyQName);

        // should only act on the standard store
        if (StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(nodeRef.getStoreRef()))
        {

            final List<ContentFingerprint> fingerprints = new ArrayList<ContentFingerprint>();

            if (this.nodeService.hasAspect(nodeRef, ContentStoresModel.ASPECT_FINGERPRINT_DATA))
            {
                // need to retrieve existing fingerprints and potentially invalidate some

                final Serializable value = this.nodeService.getProperty(nodeRef, ContentStoresModel.PROP_FINGERPRINTS);
                if (value instanceof List<?>)
                {
                    for (final Object element : (List<?>) value)
                    {
                        if (element instanceof ContentFingerprint)
                        {
                            final ContentFingerprint existingPrint = (ContentFingerprint) element;

                            // only prints for other properties remain valid
                            if (!propertyQName.equals(existingPrint.getContentProperty()))
                            {
                                fingerprints.add(existingPrint);
                            }
                        }
                    }
                }
            }

            for (final String digestType : this.digestTypes)
            {
                LOGGER.debug("Calculatng digest for type: {}", digestType);

                MessageDigest digest = null;
                try
                {
                    digest = MessageDigest.getInstance(digestType);
                }
                catch (final NoSuchAlgorithmException e1)
                {
                    LOGGER.error("Digest type is not available", e1);
                }

                if (digest != null)
                {
                    final int BUF_SIZE = 10 << 8; // 10KiB buffer
                    final byte[] buffer = new byte[BUF_SIZE];

                    final ContentReader reader = this.contentService.getReader(nodeRef, propertyQName);
                    final InputStream originalInputStream = reader.getContentInputStream();
                    try
                    {
                        while ((originalInputStream.read(buffer)) > -1)
                        {
                            digest.update(buffer);
                        }
                    }
                    catch (final IOException e)
                    {
                        LOGGER.error("Error calculating digest", e);
                    }
                    finally
                    {
                        try
                        {
                            originalInputStream.close();
                        }
                        catch (final IOException e)
                        {
                            LOGGER.warn("Failed to closed input stream", e);
                        }
                    }

                    final String digestValue = new String(Hex.encodeHex(digest.digest()));
                    fingerprints.add(new ContentFingerprint(propertyQName, digestType, digestValue));
                }
            }

            if (!fingerprints.isEmpty())
            {
                this.nodeService.addProperties(nodeRef,
                        Collections.<QName, Serializable> singletonMap(ContentStoresModel.PROP_FINGERPRINTS, (Serializable) fingerprints));
            }
            else if (this.nodeService.hasAspect(nodeRef, ContentStoresModel.ASPECT_FINGERPRINT_DATA))
            {
                this.nodeService.removeAspect(nodeRef, ContentStoresModel.ASPECT_FINGERPRINT_DATA);
            }
        }
    }
}
