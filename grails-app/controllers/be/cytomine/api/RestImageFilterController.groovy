package be.cytomine.api

import be.cytomine.processing.ImageFilter

class RestImageFilterController extends RestController {

    def imageFilterService

    def list = {
        responseSuccess(imageFilterService.list())
    }

    def show = {
        ImageFilter imageFilter = imageFilterService.read(params.id)
        if (imageFilter) responseSuccess(imageFilter)
        else responseNotFound("ImageFilter", params.id)
    }
}
