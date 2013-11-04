<#escape x as jsonUtils.encodeJSONString(x)>
{
	"nodeRef": "${nodeRef?string}",
	"modified": "${lastModified?string}",
	"mimitype": "${mimeType!""}",
	"encoding": "${encoding!""}",
	"hashValues": [
	<#if hashValues??>
		<#list hashValues as hash>
			"${hash?string}"<#if hashValues?has_next>,</#if>
		</#list>
	</#if>
	]
	<#if thumbnail??>
	,"thumbnailName": "${thumbnail}"
	</#if>
}
</#escape>