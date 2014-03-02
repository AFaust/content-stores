(function()
{
    Alfresco.util._hackathon_contentStores_generateThumbnailUrl = Alfresco.util.generateThumbnailUrl;
    Alfresco.util.generateThumbnailUrl = function(jsNode, thumbnailName)
    {
        var url = Alfresco.util._hackathon_contentStores_generateThumbnailUrl(jsNode, thumbnailName);

        if (YAHOO.lang.isString(url))
        {
            url = url.replace(Alfresco.constants.PROXY_URI, Alfresco.constants.URL_SERVICECONTEXT);
        }

        return url;
    };
}());