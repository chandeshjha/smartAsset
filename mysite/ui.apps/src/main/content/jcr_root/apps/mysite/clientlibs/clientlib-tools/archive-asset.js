(function (window,document, Granite, $) {
    "use strict";

    var foundationRegistry = $(window).adaptTo("foundation-registry");
    var ui = $(window).adaptTo("foundation-ui");
    var paths = null , titles = null;

    foundationRegistry.register("foundation.collection.action.action", {

        name: "archive.asset.action",
        handler: function (name, el, config, collection, selections) {
            paths = selections.map(function(v) {
                return $(v).data("foundationCollectionItemId");
            });
            titles = selections.map(function(v) {
                return $(v).data("foundation-collection-item-title");
            });
            if (!paths.length) return;
            handleArchiveAssets(paths,collection);  
             
        }
    });

    function handleArchiveAssets(paths,collection){
        ui.wait();

       $.ajax({
            url: Granite.HTTP.externalize("/content/nikitagargprogram/us/_jcr_content/expire.archive.json"),
            method: "POST",
            data: {
                "_charset_": "utf-8",
                "path": paths,
                "archivePath": "/content/dam/archive"
            },
            success: function () {
                if (paths.length === 1) {
                    ui.notify(undefined,
                        Granite.I18n.get("Asset <b>{0}</b> is moved to archive folder.",[titles[0]
                            ]));
                } else {
                    ui.notify(undefined,
                        Granite.I18n.get("Selected Assets are moved to archive folder."));
                }
            },
            error: function (xhr) {
                ui.alert(Granite.I18n.get('Error'),
                    Granite.I18n.get("Could not archive selected asset(s)."),
                    'error');
            },
            complete: function () {
                ui.clearWait();
            }
        });
        $(collection).adaptTo("foundation-selections").clear();     
        return;
    }

})(window,document, Granite, Granite.$);