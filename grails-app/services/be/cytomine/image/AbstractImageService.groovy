package be.cytomine.image

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.command.*
import be.cytomine.image.server.ImageProperty
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.AttachedFile
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.LinearRing
import com.vividsolutions.jts.geom.Polygon
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import grails.orm.PagedResultList

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class AbstractImageService extends ModelService {

    static transactional = false

    def commandService
    def cytomineService
    def imagePropertiesService
    def transactionService
    def storageService
    def groupService
    def imageInstanceService
    def attachedFileService
    def currentRoleServiceProxy

    def currentDomain() {
        return AbstractImage
    }

    //TODO: secure! ACL
    AbstractImage read(def id) {
        AbstractImage abstractImage = AbstractImage.read(id)
        abstractImage
    }

    //TODO: secure! ACL
    AbstractImage get(def id) {
        return AbstractImage.get(id)
    }

    //TODO: secure!
    def list(Project project) {
        ImageInstance.createCriteria().list {
            eq("project", project)
            projections {
                groupProperty("baseImage")
            }
        }
    }

    //TODO: secure! ACL
    def list(User user) {
        if(currentRoleServiceProxy.isAdminByNow(user)) {
            return AbstractImage.list()
        } else {
            def allImages = []
            def groups = groupService.list(user)
            groups.each { group ->
                allImages.addAll(list(group))

            }
            return allImages
        }
    }


    //TODO:: how to manage security here?
    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        transactionService.start()
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new AddCommand(user: currentUser)
        def res = executeCommand(c,null,json)
        //AbstractImage abstractImage = retrieve(res.data.abstractimage)
        AbstractImage abstractImage = res.object

        json.storage.each { storageID ->
            Storage storage = storageService.read(storageID)
            //CHECK WRITE ON STORAGE
            StorageAbstractImage sai = new StorageAbstractImage(storage:storage,abstractImage:abstractImage)
            sai.save(flush:true,failOnError: true)
        }
        imagePropertiesService.extractUseful(abstractImage)
        abstractImage.save(flush : true)
        //Stop transaction

        return res
    }

    //TODO:: how to manage security here?
    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(AbstractImage image,def jsonNewData) throws CytomineException {
        transactionService.start()
        SecUser currentUser = cytomineService.getCurrentUser()
        def res = executeCommand(new EditCommand(user: currentUser), image,jsonNewData)
        AbstractImage abstractImage = res.object

        if(jsonNewData.storage) {
            StorageAbstractImage.findAllByAbstractImage(abstractImage).each { storageAbstractImage ->
                def sai = StorageAbstractImage.findByStorageAndAbstractImage(storageAbstractImage.storage, abstractImage)
                sai.delete(flush:true)
            }
            jsonNewData.storage.each { storageID ->
                Storage storage = storageService.read(storageID)
                StorageAbstractImage sai = new StorageAbstractImage(storage:storage,abstractImage:abstractImage)
                sai.save(flush:true,failOnError: true)
            }
        }
        return res
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AbstractImage domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }


    def crop(params, queryString) {
        queryString = queryString.replace("?", "")
        AbstractImage abstractImage = read(params.id)
        String imageServerURL = abstractImage.getRandomImageServerURL()
        String imagePath = abstractImage.getFullPath()
        return "$imageServerURL/image/crop.png?fif=$imagePath&$queryString" //&scale=$scale
    }

    def tile(def params, String queryString) {
        AbstractImage abstractImage = read(params.id)
        int tileGroup = params.int("TileGroup")
        int x = params.int("x")
        int y = params.int("y")
        int z = params.int("z")
        String imageServerURL = abstractImage.getRandomImageServerURL()
        String imagePath = abstractImage.getFullPath()
        def zoomifyQuery = "zoomify=$imagePath/TileGroup$tileGroup/$z-$x-$y\\.jpg"
        return "$imageServerURL/image/tile.jpg?$zoomifyQuery"
    }

    def window(def params, String queryString) {
        Long id = params.long('id')
        AbstractImage abstractImage = read(id)
        int x = params.int('x')
        int y = params.int('y')
        int w = params.int('w')
        int h = params.int('h')
        def boundaries = [:]
        boundaries.topLeftX = x
        boundaries.topLeftY = abstractImage.getHeight() - h
        boundaries.width = w
        boundaries.height = h
        boundaries.imageWidth = abstractImage.getWidth()
        boundaries.imageHeight = abstractImage.getHeight()
        if (params.zoom) boundaries.zoom = params.zoom
        if (params.maxSize) boundaries.maxSize = params.maxSize
        return UrlApi.getCropURL(id, boundaries)
    }



    /**
     * Extract image properties from file for a specific image
     */
    def imageProperties(AbstractImage abstractImage) {
        if (!ImageProperty.findByImage(abstractImage)) {
            imagePropertiesService.populate(abstractImage)
        }
        return ImageProperty.findAllByImage(abstractImage)
    }

    /**
     * Get a single property thx to its id
     */
    def imageProperty(long imageProperty) {
        return ImageProperty.findById(imageProperty)
    }

    /**
     * Get all image servers for an image id
     */
    def imageServers(def id) {
        AbstractImage image = read(id)
        def urls = []
        for (imageServerStorage in image.getImageServersStorage()) {
            urls << [imageServerStorage.getZoomifyUrl(), image.getPath()].join(File.separator) + "/"
        }
        return [imageServersURLs : urls]
    }

    def getCropURL(AbstractImage abstractImage, def boundaries) {

    }

    /**
     * Get thumb image URL
     */
    def thumb(long id, int maxSize) {
        AbstractImage abstractImage = AbstractImage.read(id)
        String imageServerURL = abstractImage.getRandomImageServerURL()
        UploadedFile uploadedFile = getMainUploadedFile(abstractImage)
        String fif = uploadedFile.absolutePath
        fif = fif.replace(" ", "%20")
        String mimeType = uploadedFile.mimeType
        String uri = "$imageServerURL/image/thumb.jpg?fif=$fif&mimeType=$mimeType&maxSize=$maxSize"
        println uri
        AttachedFile attachedFile = AttachedFile.findByDomainIdentAndFilename(id, uri)
        if (attachedFile) {
            return ImageIO.read(new ByteArrayInputStream(attachedFile.getData()))
        } else {
            byte[] imageData = new URL(uri).getBytes()
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData))
            attachedFileService.add(uri, imageData, abstractImage.id, AbstractImage.class.getName())
            return bufferedImage
        }

    }

    /**
     * Get Preview image URL
     */
    def preview(def id) {
        thumb(id, 1024)
    }

    def getMainUploadedFile(AbstractImage abstractImage) {
        UploadedFile uploadedfile = UploadedFile.findByImage(abstractImage)
        if (uploadedfile && uploadedfile.parent && uploadedfile.ext != "zip") return uploadedfile.parent
        else return uploadedfile
    }

    def downloadURI(AbstractImage abstractImage) {
        String fif = UploadedFile.findByImage(abstractImage)?.downloadParent?.absolutePath
        if (fif) {
            String imageServerURL = abstractImage.getRandomImageServerURL()
            return "$imageServerURL/image/download?fif=$fif"
        } else {
            //return 404
        }

    }

    def getAvailableAssociatedImages(AbstractImage abstractImage) {
        String imageServerURL = abstractImage.getRandomImageServerURL()
        UploadedFile uploadedFile = getMainUploadedFile(abstractImage)
        String fif = uploadedFile.absolutePath
        fif = fif.replace(" ", "%20")
        String mimeType = uploadedFile.mimeType
        String uri = "$imageServerURL/image/associated?fif=$fif&mimeType=$mimeType"
        return JSON.parse( new URL(uri).text )
    }

    def getAssociatedImage(AbstractImage abstractImage, String label, def maxWidth) {
        String imageServerURL = abstractImage.getRandomImageServerURL()
        UploadedFile uploadedFile = getMainUploadedFile(abstractImage)
        String fif = uploadedFile.absolutePath
        fif = fif.replace(" ", "%20")
        String mimeType = uploadedFile.mimeType
        String uri = "$imageServerURL/image/nested?fif=$fif&mimeType=$mimeType&label=$label"
        AttachedFile attachedFile = AttachedFile.findByDomainIdentAndFilename(abstractImage.id, uri)
        if (attachedFile) {
            return ImageIO.read(new ByteArrayInputStream(attachedFile.getData()))
        } else {
            byte[] imageData = new URL(uri).getBytes()
            BufferedImage bufferedImage =  ImageIO.read(new ByteArrayInputStream(imageData))
            attachedFileService.add(uri, imageData, abstractImage.id, AbstractImage.class.getName())
            return bufferedImage
        }

    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.originalFilename]
    }

    def deleteDependentImageInstance(AbstractImage ai, Transaction transaction,Task task=null) {
        def images = ImageInstance.findAllByBaseImage(ai)
        if(!images.isEmpty()) {
            throw new WrongArgumentException("You cannot delete this image, it has already been insert in projects " + images.collect{it.project.name})
        }
    }

    def deleteDependentAttachedFile(AbstractImage ai, Transaction transaction,Task task=null) {
        AttachedFile.findAllByDomainIdentAndDomainClassName(ai.id, ai.class.getName()).each {
            attachedFileService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentImageProperty(AbstractImage ai, Transaction transaction,Task task=null) {
        //TODO: implement imagePropertyService with command
        imagePropertiesService.clear(ai)
    }

    def deleteDependentNestedFile(AbstractImage ai, Transaction transaction,Task task=null) {
        //TODO: implement this with command (nestedFileService should be create)
        NestedFile.findAllByAbstractImage(ai).each {
            it.delete(flush: true)
        }
    }

    def deleteDependentStorageAbstractImage(AbstractImage ai, Transaction transaction,Task task=null) {
        //TODO: implement this with command (storage abst image should be create)
        StorageAbstractImage.findAllByAbstractImage(ai).each {
            it.delete(flush: true)
        }
    }

    def deleteDependentNestedImageInstance(AbstractImage ai, Transaction transaction,Task task=null) {
        NestedImageInstance.findAllByBaseImage(ai).each {
            it.delete(flush: true)
        }
    }
}
