content-stores
==============

Alfresco Summit 2013 - Barcelona Hack-a-thon project for developing additional content store implementations for Alfresco


HashBasedFileContentStore
=========================

Implements a variant of the FileContentStore that uses a message digest based directory structure instead of the date based one. 
This has the advantage that it provides de-deplication out of the box. To configure it instead of the default Alfresco
FileContentStore use this configuration:

```xml
	<bean id="hashBasedFileContentStore" class="org.alfresco.hackathon.content.stores.repo.HashBasedFileContentStore" >
        <property name="rootDirectory" value="${dir.contentstore}" />
        <property name="messageDigestType" value="SHA-512" />
	</bean>	
	
	<!-- Point the ContentService to the 'selector' store -->
	<bean id="contentService" parent="baseContentService">
		<property name="store">
			<ref bean="hashBasedFileContentStore" />
		</property>
	</bean>
```

There is also a sample XML configuration file alfresco/WEB-INF/classes/alfresco/extension/hash-based-content-store-context.xml.sample
which overrides the default file content store and only needs to be copied into the shared/classes/alfresco/extension directory and
be renamed to remove the .sample suffix.