<#escape x as jsonUtils.encodeJSONString(x)>
{
	"nodeRef": "${nodeRef?string}",
	"modified": "${lastModified?string}",
	"mimetype": "${mimeType!""}",
	"encoding": "${encoding!""}",
	"hashValues": [
	<#if hashValues??>
		<#list hashValues as hash>
			"${hash}"<#if hashValues_has_next>,</#if>
		</#list>
	</#if>
	]
	<#if thumbnail??>
	,"thumbnailName": "${thumbnail}"
	</#if>
}
</#escape>