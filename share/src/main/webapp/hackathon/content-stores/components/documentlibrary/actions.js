(function()
{
    if (YAHOO.lang.isObject(Alfresco.doclib.Actions) && YAHOO.lang.isObject(Alfresco.doclib.Actions.prototype))
    {
        Alfresco.doclib.Actions.prototype._hackathon_contentStores_getActionUrls = Alfresco.doclib.Actions.prototype.getActionUrls;
        Alfresco.doclib.Actions.prototype.getActionUrls = function(record, siteId)
        {
            var actionUrls = this._hackathon_contentStores_getActionUrls(record, siteId), contentUrl = record.jsNode.contentURL;

            actionUrls.downloadUrl = Alfresco.util.combinePaths(Alfresco.constants.URL_SERVICECONTEXT, contentUrl) + "?a=true";
            actionUrls.viewUrl = Alfresco.util.combinePaths(Alfresco.constants.URL_SERVICECONTEXT, contentUrl) + "\" target=\"_blank";

            return actionUrls;
        };
    }
}());