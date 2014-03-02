package org.alfresco.hackathon.content.stores.repo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.node.NodePropertyValue;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.cmr.repository.datatype.TypeConverter.Converter;
import org.alfresco.service.namespace.QName;

public class ContentFingerprint implements Serializable
{

    private static final long serialVersionUID = 5841016412118343530L;

    static
    {
        DefaultTypeConverter.INSTANCE.addConverter(ContentFingerprint.class, String.class, new Converter<ContentFingerprint, String>()
        {
            public String convert(final ContentFingerprint source)
            {
                final String value = MessageFormat.format("%s|%s|%s", source.getContentProperty().toString(),
                        source.getMessageDigestType(), source.getDigestValue());
                return null;
            }
        });

        DefaultTypeConverter.INSTANCE.addConverter(String.class, ContentFingerprint.class, new Converter<String, ContentFingerprint>()
        {
            public ContentFingerprint convert(final String source)
            {
                final String[] fragments = source.split("\\|");
                final ContentFingerprint fingerprint;
                if (fragments.length == 3)
                {
                    fingerprint = new ContentFingerprint(QName.createQName(fragments[0]), fragments[1], fragments[2]);
                }
                else
                {
                    throw new TypeConversionException("Failed to parse content fingerprint " + source);
                }
                return fingerprint;
            }
        });

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

    private final QName contentProperty;
    private final String messageDigestType;
    private final String digestValue;


    /**
     * Constructor for serialization
     */
    protected ContentFingerprint()
    {
        this.contentProperty = null;
        this.messageDigestType = null;
        this.digestValue = null;
    }

    /**
     * Constructs a new content fingerprint instance
     *
     * @param contentProperty
     *            the property holding the fingerprinted content
     * @param messageDigestType
     *            the type of digest used
     * @param digestValue
     *            the value of the digest (most commonly in hex-based format)
     */
    public ContentFingerprint(final QName contentProperty, final String messageDigestType, final String digestValue)
    {
        this.contentProperty = contentProperty;
        this.messageDigestType = messageDigestType;
        this.digestValue = digestValue;
    }

    /**
     * @return the contentProperty
     */
    public final QName getContentProperty()
    {
        return this.contentProperty;
    }

    /**
     * @return the messageDigestType
     */
    public final String getMessageDigestType()
    {
        return this.messageDigestType;
    }

    /**
     * @return the digestValue
     */
    public final String getDigestValue()
    {
        return this.digestValue;
    }

}
