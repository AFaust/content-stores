<#escape x as jsonUtils.encodeJSONString(x)>
{
	"nodeRef": "${node.nodeRef?string}",
	"modified": "${xmldate(node.properties["cm:modified"])}",
	"mimetype": "${node.properties[contentProperty].mimetype}",
	"encoding": "${node.properties[contentProperty].encoding}",
    "fingerprints": [
	<#if node.properties["hack:fingerprints"]??>
        <#assign idx = 0 />
		<#list node.properties["hack:fingerprints"] as fingerprint>
            <#if node.properties[contentProperty] == node.properties[fingerprint.contentProperty]>
            <#if idx != 0>,</#if>{
                "contentProperty" : "${contentProperty}",
                "digestType": "${fingerprint.messageDigestType}",
                "digestValue": "${fingerprint.digestValue}"
			}
			<#assign idx = idx + 1 />
			</#if>
		</#list>
	</#if>
	]
}
</#escape>