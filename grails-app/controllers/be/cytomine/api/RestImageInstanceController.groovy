package be.cytomine.api

import be.cytomine.image.ImageInstance
import be.cytomine.security.User
import be.cytomine.image.AbstractImage
import be.cytomine.project.Project
import be.cytomine.ontology.Annotation
import be.cytomine.ontology.AnnotationTerm
import java.awt.image.BufferedImage
import be.cytomine.ontology.Term
import java.awt.Color
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.LinearRing
import be.cytomine.Exception.CytomineException
/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 18/05/11
 */
class RestImageInstanceController extends RestController {

    def springSecurityService
    def transactionService
    def segmentationService
    def imageInstanceService
    def projectService
    def abstractImageService
    def userService

    def index = {
        redirect(controller: "image")
    }
    def list = {
        response(imageInstanceService.list())
    }

    def show = {
        ImageInstance image = imageInstanceService.read(params.id)
        if (image) responseSuccess(image)
        else responseNotFound("ImageInstance", params.id)
    }

    def showByProjectAndImage = {
        Project project = projectService.read(params.idproject)
        AbstractImage image = abstractImageService.read(params.idimage)
        ImageInstance imageInstance = imageInstanceService.get(image, project)
        if (imageInstance) responseSuccess(imageInstance)
        else responseNotFound("ImageInstance", "Project", params.idproject, "Image", params.idimage)
    }

    def listByUser = {
        User user = userService.read(params.id)
        if (user) responseSuccess(imageInstanceService.list(user))
        else responseNotFound("ImageInstance", "User", params.id)
    }

    def listByImage = {
        AbstractImage image = abstractImageService.read(params.id)
        if (image) responseSuccess(imageInstanceService.list(image))
        else responseNotFound("ImageInstance", "AbstractImage", params.id)
    }

    def listByProject = {
        Project project = projectService.read(params.id)
        def images = imageInstanceService.list(project)

        if (project) responseSuccess(images)
        else responseNotFound("ImageInstance", "Project", params.id)
    }

    def add = {
        try {
            def result = imageInstanceService.add(request.JSON)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.message], e.code)
        } finally {
            transactionService.stopIfTransactionInProgress()
        }
    }

    def update = {
        try {
            def result = imageInstanceService.update(request.JSON)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.message], e.code)
        } finally {
            transactionService.stopIfTransactionInProgress()
        }
    }

    def delete = {
        try {
            def result = imageInstanceService.delete(params.idproject, params.idimage)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.message], e.code)
        } finally {
            transactionService.stopIfTransactionInProgress()
        }
    }

    def window = {
        println "WINDOW REQUEST " + params.toString()
        ImageInstance image = ImageInstance.read(params.id)
        AbstractImage abstractImage = image.getBaseImage()
        int x = Integer.parseInt(params.x)
        int y = abstractImage.getHeight() - Integer.parseInt(params.y)
        int w = Integer.parseInt(params.w)
        int h = Integer.parseInt(params.h)
        try {
            String url = abstractImage.getCropURL(x, y, w, h)
            log.info("Window : " + url)
            responseImage(url)
        } catch (Exception e) {
            log.error("GetThumb:" + e);
        }
    }

    def mask = {
        println "WINDOW REQUEST " + params.toString()
        ImageInstance image = ImageInstance.read(params.id)
        AbstractImage abstractImage = image.getBaseImage()
        int x = Integer.parseInt(params.x)
        int y = Integer.parseInt(params.y)
        int w = Integer.parseInt(params.w)
        int h = Integer.parseInt(params.h)
        int termID = Integer.parseInt(params.term)

        try {
            //Get the image, compute ratio between asked and received
            String url = abstractImage.getCropURL(x, abstractImage.getHeight() - y, w, h)
            BufferedImage window = getImageFromURL(url)
            double x_ratio = window.getWidth() / w
            double y_ratio = window.getHeight() / h

            //Fetch annotations with the requested term on the request image
            Term term = Term.read(termID)

            Collection<Annotation> annotations = (Collection<Annotation>) AnnotationTerm.createCriteria().list {
                inList("term", [term])
                join("annotation")
                createAlias("annotation", "a")
                projections {
                    inList("a.image", [image])
                    groupProperty("annotation")
                }
            }
            //Create a geometry corresponding to the ROI of the request (x,y,w,h)
            //1. Compute points
            Coordinate[] roiPoints = new Coordinate[5]
            roiPoints[0] = new Coordinate(x, abstractImage.getHeight() - y);
            roiPoints[1] = new Coordinate(x + w, abstractImage.getHeight() - y);
            roiPoints[2] = new Coordinate(x + w, abstractImage.getHeight() - (y + h));
            roiPoints[3] = new Coordinate(x, abstractImage.getHeight() - (y + h));
            roiPoints[4] = roiPoints[0]
            //Build geometry
            LinearRing linearRing = new GeometryFactory().createLinearRing(roiPoints)
            Geometry roiGeometry = new GeometryFactory().createPolygon(linearRing)
            //Filter annotation which intersects the ROI
            Collection<Geometry> intersectGeometries = new LinkedList<Geometry>()
            annotations.each { annotation ->
                if (roiGeometry.intersects(annotation.getLocation())) {
                    intersectGeometries.add(annotation.getLocation())
                }
            }
            window = segmentationService.colorizeWindow(abstractImage, window, intersectGeometries, Color.decode(term.color), x, y, x_ratio, y_ratio)
            responseBufferedImage(window)
        } catch (Exception e) {
            log.error("GetThumb:" + e);
        }
    }
}
