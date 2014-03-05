<#include "/org/alfresco/components/form/controls/common/utils.inc.ftl" />

<#assign fieldValue=field.value>

<#if fieldValue?string == "" && field.control.params.defaultValueContextProperty??>
    <#if context.properties[field.control.params.defaultValueContextProperty]??>
        <#assign fieldValue = context.properties[field.control.params.defaultValueContextProperty] />
    <#elseif args[field.control.params.defaultValueContextProperty]??>
        <#assign fieldValue = args[field.control.params.defaultValueContextProperty] />
    </#if>
</#if>

<#if fieldValue?string != "">
    <#assign values=fieldValue?split(",") />
<#else>
    <#assign values=[] />
</#if>

<div class="form-field">
    <#if form.mode == "view">
        <div class="viewmode-field">
            <#if field.mandatory && !(fieldValue?is_number) && fieldValue?string == "">
                <span class="incomplete-warning"><img src="${url.context}/res/components/form/images/warning-16.png" title="${msg("form.field.incomplete")}" /><span>
            </#if>
            <span class="viewmode-label">${field.label?html}:</span>
            <#if fieldValue?string == "">
                <span class="viewmode-value">${msg("form.control.novalue")?html}</span>
            <#else>
                <span class="viewmode-value">
                    <#list values as value>
                        <#assign fragments = value?split("|") />
                        <span class="single-content-fingerprint">${fragments[1]?html} ${fragments[2]?html}</span>
                    </#list>
                </span>
            </#if>
        </div>
    </#if>
</div>