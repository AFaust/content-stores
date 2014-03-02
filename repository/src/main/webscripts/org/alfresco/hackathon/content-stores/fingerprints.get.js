function main()
{
    var nodeRef, node, contentPropOrThumbnailName, thumbnail;

    nodeRef = url.templateArgs.storeType + "://" + url.templateArgs.storeId + "/" + url.templateArgs.id;
    node = search.findNode(nodeRef);

    if (node !== undefined && node !== null)
    {
        contentPropOrThumbnailName = url.templateArgs.contentPropOrThumbnailName || "cm:content";

        if (node.properties[contentPropOrThumbnailName] !== null)
        {
            model.node = node;
            model.contentProperty = contentPropOrThumbnailName;
        }
        else
        {
            thumbnail = node.getThumbnail(contentPropOrThumbnailName);
            if (thumbnail !== undefined && thumbnail !== null)
            {
                model.node = thumbnail;
                model.contentProperty = "cm:content";
            }
            else
            {
                status.code = status.STATUS_NOT_FOUND;
                status.message = "Thumbnail or content property " + contentPropOrThumbnailName + " of " + nodeRef + " does not exist";
                status.redirect = true;
            }
        }
    }
    else
    {
        status.code = status.STATUS_NOT_FOUND;
        status.message = nodeRef + " does not exist";
        status.redirect = true;
    }
}

main();