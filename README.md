content-stores
==============

Alfresco Summit 2013 - Barcelona Hack-a-thon project for developing additional content store implementations for Alfresco


HashBasedFileContentStore
=========================

Implements a variant of the FileContentStore that uses a SHA1 based directory structure instead of the date based one. 
This has the advantage that it provides de-deplication out of the box. To configure it instead of the default Alfresco
FileContentStore use this configuration:

```xml
	<bean id="hashBasedFileContentStore" class="org.alfresco.hackathon.HashBasedFileContentStore" >
	  <constructor-arg>
         <value>${dir.contentstore}</value>
    </constructor-arg>
	</bean>	
	
	<!-- Point the ContentService to the 'selector' store -->
	<bean id="contentService" parent="baseContentService">
		<property name="store">
			<ref bean="hashBasedFileContentStore" />
		</property>
	</bean>
```
