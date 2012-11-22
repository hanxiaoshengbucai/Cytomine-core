var ExplorerTabs = Backbone.View.extend({
    tagName:"div",
    triggerRoute:true,
    /**
     * ExplorerTabs constructor
     * @param options
     */
    initialize:function (options) {
        this.tabs = []; //that we are browsing
        this.container = options.container;
        this.dashboard = null;
    },
    /**
     * Grab the layout and call ask for render
     */
    render:function () {
        var self = this;
        require(["text!application/templates/explorer/Tabs.tpl.html"], function (tpl) {
            self.doLayout(tpl);
        });
        return this;
    },
    /**
     * Render the html into the DOM element associated to the view
     * @param tpl
     */
    doLayout:function (tpl) {
        var self = this;
        $(this.el).html(_.template(tpl, {}));
        return this;
    },
    /**
     *  Add a Tab containing a BrowseImageView instance
     *  @idImage : the id of the Image we want to display
     *  @options : some init options we want to pass the the BrowseImageView Instance
     */
    addBrowseImageView:function (idImage, options) {
        var self = this;
        var tab = this.getImageView(idImage);
        if (tab != null) {
            tab.view.show(options);
            self.showTab(idImage,"image");
            return;
        }

        var tabs = $("#explorer-tab-content");
        var view = new BrowseImageView({
            initCallback:function () {
                view.show(options)
            },
            el:tabs
        });
        self.tabs.push({idImage:idImage, view:view});

        new ImageInstanceModel({id:idImage}).fetch({
            success:function (model, response) {
                view.model = model;
                view.render();
                $(".closeTab").on("click", function (e) {
                    var idImage = $(this).attr("data-image");
                    self.removeTab(idImage,"image");
                });
                self.showTab(idImage,"image");

            }
        });
    },
    addReviewImageView:function (idImage, options) {
        console.log("addReviewImageView:"+idImage);
        var self = this;
        var tab = this.getImageView("review-"+idImage);
        if (tab != null) {
            tab.view.show(options);
            self.showTab(idImage,"review");
            return;
        }

        var tabs = $("#explorer-tab-content");
        var view = new BrowseImageView({
            initCallback:function () {
                view.show(options)
            },
            el:tabs,
            review : true
        });
        self.tabs.push({idImage:"review-"+idImage, view:view});

        new ImageInstanceModel({id:idImage}).fetch({
            success:function (model, response) {
                view.model = model;
                view.render();
                $(".closeTab").on("click", function (e) {
                    var idImage = $(this).attr("data-image");
                    self.removeTab(idImage,"review");
                });
                self.showTab(idImage,"review");

                if(model.get("inReview")==false && model.get("reviewed")==false) {

                    self.removeTab(idImage,"review");
                    window.app.view.message("Review image", "You must first start reviewing picture before review it!", "warning");
                }
            }
        });
    },

    /**
     * Return the reference to a BrowseImageView instance
     * contained in a tab
     * @param idImage the ID of an Image contained in a BrowseImageView
     */
    getImageView:function (idImage) {
        var object = _.detect(this.tabs, function (object) {
            return object.idImage == idImage;
        });
        return object != null ? object : null;
    },
    /**
     * Remove a Tab
     * @param index the identifier of the Tab
     */
    removeTab:function (idImage, prefix) {
        var browseImageView = null
        if(prefix!="review") browseImageView = this.getImageView(idImage);
        else browseImageView = this.getImageView("review-"+idImage);
        browseImageView.view.stopBroadcastingInterval();
        browseImageView.view.stopWatchOnlineUsersInterval();
        var indexOf = this.tabs.indexOf(browseImageView);

        this.tabs.splice(indexOf, 1);
        var tabs = $(this.el).children('.nav-tab');
        //Remove Tab
        $('#tabs-'+prefix+'-' + idImage).parent().remove();
        //Remove dropdown
        $('#tabs-'+prefix+'-' + idImage + "-dropdown").parent().remove();
        //Remove content
        $('#tabs-'+prefix+'-' + window.app.status.currentProject + '-' + idImage + '-').remove();
    },
    /**
     * Show a tab
     * @param idImage the identifier of the Tab
     */
    showTab:function (idImage, prefix) {
        var tabs = $("#explorer > .browser").find(".nav-tabs");
        window.app.controllers.browse.tabs.triggerRoute = false;
        $('#tabs-'+prefix+'-' + idImage).click();
        window.app.controllers.browse.tabs.triggerRoute = true;
    },
    /**
     * Return the number of opened tabs
     */
    size:function () {
        return _.size(this.tabs);
    },
    /**
     * Close all the Tabs
     */
    closeAll:function () {
        var tabs = $(this.el).children('.tabs');
        this.tabs = [];
        $(this.el).hide();
        $(this.el).parent().find('.noProject').show();
    },
    /**
     * Add a ProjectDashBoardView instance in the first Tab
     * @param dashboard the ProjectDashBoardView instance
     */
    addDashboard:function (dashboard) {
        var self = this;
        this.dashboard = dashboard;
        var tabs = $('#explorer-tab-content');
        $(".nav-tabs").append(_.template("<li><a id='dashboardLink-<%= idProject %>' href='#tabs-dashboard-<%= idProject %>' data-toggle='tab'><i class='icon-road' /> Dashboard</a></li>", { idProject:window.app.status.currentProject}));
        $(".nav-tabs").append(_.template("<li><a href='#tabs-images-<%= idProject %>' data-toggle='tab'><i class='icon-picture' /> Images</a></li>", { idProject:window.app.status.currentProject}));
        $(".nav-tabs").append(_.template("<li><a href='#tabs-annotations-<%= idProject %>' data-toggle='tab'><i class='icon-pencil' /> Annotations</a></li>", { idProject:window.app.status.currentProject}));
        $(".nav-tabs").append(_.template("<li><a href='#tabs-algos-<%= idProject %>' data-toggle='tab'><i class='icon-tasks' /> Jobs</a></li>", { idProject:window.app.status.currentProject}));
        $(".nav-tabs").append(_.template("<li><a href='#tabs-config-<%= idProject %>' data-toggle='tab'><i class='icon-wrench' /> Configuration</a></li>", { idProject:window.app.status.currentProject}));

        $('a[data-toggle="tab"]').live('show', function (e) {
            var hash = this.href.split("#")[1];
            $("#" + hash).attr('style', 'width:100%;height:500;overflow:hidden;');
            if (self.triggerRoute) window.app.controllers.browse.navigate("#" + hash, self.triggerRoute);
        });

        $("#explorer > .browser").show();
        $("#explorer > .noProject").hide();
    },
    /**
     * Ask to the dashboard view to refresh
     */
    getDashboard:function () {
        return this.dashboard;
    }
});
