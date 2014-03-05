(function()
{
    if (YAHOO.lang.isFunction(Alfresco.WebPreview))
    {
        Alfresco.WebPreview.prototype._hackathon_contentStores_getContentUrl = Alfresco.WebPreview.prototype.getContentUrl;
        Alfresco.WebPreview.prototype._hackathon_contentStores_getThumbnailUrl = Alfresco.WebPreview.prototype.getThumbnailUrl;

        Alfresco.WebPreview.prototype.getContentUrl = function(download)
        {
            var url = this._hackathon_contentStores_getContentUrl(download);

            if (this.options.api === "api" && YAHOO.lang.isString(url))
            {
                url = url.replace(Alfresco.constants.PROXY_URI_RELATIVE, Alfresco.constants.URL_SERVICECONTEXT);
            }

            return url;
        };

        Alfresco.WebPreview.prototype.getThumbnailUrl = function(thumbnail, fileSuffix)
        {
            var url = this._hackathon_contentStores_getThumbnailUrl(thumbnail, fileSuffix);

            if (this.options.api === "api" && YAHOO.lang.isString(url))
            {
                url = url.replace(Alfresco.constants.PROXY_URI_RELATIVE, Alfresco.constants.URL_SERVICECONTEXT);
            }

            return url;
        };
    }
}());