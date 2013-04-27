import be.cytomine.web.CytomineMultipartHttpServletRequest

// Place your Spring DSL code here
beans = {
    'apiAuthentificationFilter'(cytomine.web.APIAuthentificationFilters) {
        // properties
    }
    'multipartResolver'(CytomineMultipartHttpServletRequest) {
        // Max in memory 100kbytes
        maxInMemorySize=10240*3

        //3Gb Max upload size
        maxUploadSize=1024000000*3
    }
}