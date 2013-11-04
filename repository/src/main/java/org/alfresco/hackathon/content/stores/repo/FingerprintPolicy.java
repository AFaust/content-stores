package org.alfresco.hackathon.content.stores.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

public class FingerprintPolicy implements
		ContentServicePolicies.OnContentUpdatePolicy {

	private static Logger log = Logger.getLogger(FingerprintPolicy.class);

	private PolicyComponent policyComponent;

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	private ServiceRegistry serviceRegistry;

	private String digestTypes;
	private String propertiesToIndex;

	public void init() {
		this.policyComponent.bindClassBehaviour(
				OnContentUpdatePolicy.QNAME,
				ContentModel.TYPE_CONTENT, new JavaBehaviour(this,
						"onContentUpdate",
						Behaviour.NotificationFrequency.EVERY_EVENT));
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setDigestTypes(String digestTypes) {
		this.digestTypes = digestTypes;
	}

	public void setPropertiesToIndex(String propertiesToIndex) {
		this.propertiesToIndex = propertiesToIndex;
	}

	public void onContentUpdate(NodeRef nodeRef, boolean trueFalse) {

		List<String> digestTypesL = new ArrayList<String>();
		for (String digestType : this.digestTypes.split("|")) {
			digestTypesL.add(digestType);
		}

		List<String> propertiesToIndexL = new ArrayList<String>();
		for (String propertyToIndex : this.propertiesToIndex.split("|")) {
			propertiesToIndexL.add(propertyToIndex);
		}

		log.debug("onUpdateNode workspace://SpacesStore/" + nodeRef.getId());

		ContentService contentService = serviceRegistry.getContentService();
		NamespaceService namespaceService = serviceRegistry
				.getNamespaceService();
		NodeService nodeService = serviceRegistry.getNodeService();

		List<String> fingerprints = new ArrayList<String>();

		for (String digestType : digestTypesL) {
			log.debug("Digest type: " + digestType);

			for (String propertyToIndex : propertiesToIndexL) {

				MessageDigest md = null;
				try {
					md = MessageDigest.getInstance(digestType);
				} catch (NoSuchAlgorithmException e1) {
					log.error(e1.getLocalizedMessage());
				}

				QName qProp = QName.createQName(propertyToIndex,
						namespaceService);

				ContentReader reader = contentService.getReader(nodeRef, qProp);
				InputStream originalInputStream = reader
						.getContentInputStream();
				final int BUF_SIZE = 10 << 8; // 10KiB buffer
				byte[] buffer = new byte[BUF_SIZE];

				try {
					while ((originalInputStream.read(buffer)) > -1) {
						md.update(buffer);
					}
					originalInputStream.close();
				} catch (IOException e) {
					log.error(e.getLocalizedMessage());
				}

				String hash = new String(Hex.encodeHex(md.digest()));

				fingerprints.add(String.format("%s|%s|%s", propertyToIndex,
						digestType, hash));

			}
		}

		QName CUSTOM_ASPECT_QNAME = QName
				.createQName("hack:fingerprintDataAspect");
		QName PROP_QNAME_MY_PROPERTY = QName.createQName("hack:fingerprints");
		Map<QName, Serializable> aspectValues = new HashMap<QName, Serializable>();
		aspectValues.put(PROP_QNAME_MY_PROPERTY,
				fingerprints.toArray(new String[fingerprints.size()]));
		nodeService.addAspect(nodeRef, CUSTOM_ASPECT_QNAME, aspectValues);
	}


}
