(function (window,document, Granite, $) {
    "use strict";

    var foundationRegistry = $(window).adaptTo("foundation-registry");
    var ui = $(window).adaptTo("foundation-ui");
    var paths = null,titles = null;

    foundationRegistry.register("foundation.collection.action.action", {

        name: "expire.asset.action",
        handler: function (name, el, config, collection, selections) {
            paths = selections.map(function(v) {
                return $(v).data("foundationCollectionItemId");
            });
            titles = selections.map(function(v) {
                return $(v).data("foundation-collection-item-title");
            }   );
            if (!paths.length) return;

            handleExpireAssets(paths,collection);    
             
        }
    });

    function handleExpireAssets(paths,collection){
        ui.wait();

       $.ajax({
            url: Granite.HTTP.externalize("/content/nikitagargprogram/us/_jcr_content/expire.update-expiration.json"),
            method: "POST",
            data: {
                "_charset_": "utf-8",
                "path": paths
            },
            success: function () {
                if (paths.length === 1) {
                    ui.notify(undefined,
                        Granite.I18n.get("Asset <b>{0}</b> is set to Expire Today.",
                            titles[0]));
                } else {
                    ui.notify(undefined,
                        Granite.I18n.get("Selected Assets expiration date is set to Today."));
                }
            },
            error: function (xhr) {
                ui.alert(Granite.I18n.get('Error'),
                    Granite.I18n.get("Could not update expiration date for asset(s)."),
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