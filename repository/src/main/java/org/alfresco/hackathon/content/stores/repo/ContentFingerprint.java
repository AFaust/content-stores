package org.alfresco.hackathon.content.stores.repo;

import java.io.Serializable;
import java.text.MessageFormat;

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
                final String value = MessageFormat.format("{0}|{1}|{2}", source.getContentProperty().toString(),
                        source.getMessageDigestType(), source.getDigestValue());
                return value;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.contentProperty == null) ? 0 : this.contentProperty.hashCode());
        result = prime * result + ((this.digestValue == null) ? 0 : this.digestValue.hashCode());
        result = prime * result + ((this.messageDigestType == null) ? 0 : this.messageDigestType.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof ContentFingerprint))
        {
            return false;
        }
        final ContentFingerprint other = (ContentFingerprint) obj;
        if (this.contentProperty == null)
        {
            if (other.contentProperty != null)
            {
                return false;
            }
        }
        else if (!this.contentProperty.equals(other.contentProperty))
        {
            return false;
        }
        if (this.digestValue == null)
        {
            if (other.digestValue != null)
            {
                return false;
            }
        }
        else if (!this.digestValue.equals(other.digestValue))
        {
            return false;
        }
        if (this.messageDigestType == null)
        {
            if (other.messageDigestType != null)
            {
                return false;
            }
        }
        else if (!this.messageDigestType.equals(other.messageDigestType))
        {
            return false;
        }
        return true;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final String toString = DefaultTypeConverter.INSTANCE.convert(String.class, this);
        return toString;
    }
}
