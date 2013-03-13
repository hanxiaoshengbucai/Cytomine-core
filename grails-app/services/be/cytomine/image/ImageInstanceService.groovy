package be.cytomine.image

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.SecurityACL
import be.cytomine.SecurityCheck
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.social.UserPosition
import be.cytomine.utils.ModelService
import grails.converters.JSON
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONObject
import org.hibernate.FetchMode
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.utils.Task
import static org.springframework.security.acls.domain.BasePermission.*

/**
 * TODO:: refactor + doc!!!!!!!
 */
class ImageInstanceService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def userAnnotationService
    def algoAnnotationService
    def dataSource
    def reviewedAnnotationService

    def currentDomain() {
        return ImageInstance
    }

    def read(def id) {
        def image = ImageInstance.read(id)
        if(image) {
            SecurityACL.check(image.container(),READ)
        }
        image
    }

    def get(def id) {
        def image = ImageInstance.get(id)
        if(image) {
            SecurityACL.check(image.container(),READ)
        }
        image
    }

    def list(Project project) {
        SecurityACL.check(project,READ)

        def images = ImageInstance.createCriteria().list {
            createAlias("baseImage", "i")
            eq("project", project)
            order("i.created", "desc")
            fetchMode 'baseImage', FetchMode.JOIN
        }
        return images
    }

    def listTree(Project project) {
        SecurityACL.check(project,READ)

        def children = []
        list(project).each { image->
            children << [ id : image.id, key : image.id, title : image.baseImage.originalFilename, isFolder : false, children : []]
        }
        def tree = [:]
        tree.isFolder = true
        tree.hideCheckbox = true
        tree.name = project.getName()
        tree.title = project.getName();
        tree.key = project.getId()
        tree.id = project.getId()
        tree.children = children
        return tree
    }

    def list(Project project, int inf, int sup) {
        SecurityACL.check(project,READ)

        def images = ImageInstance.createCriteria().list(max:sup-inf, offset:inf) {
            createAlias("baseImage", "i")
            eq("project", project)
            order("created", "desc")
            fetchMode 'baseImage', FetchMode.JOIN
        }
        return images
    }

    /**
     * Get all image id from project
     */
    public List<Long> getAllImageId(Project project) {
        SecurityACL.check(project,READ)

        //better perf with sql request
        String request = "SELECT a.id FROM image_instance a WHERE project_id="+project.id
        def data = []
        new Sql(dataSource).eachRow(request) {
            data << it[0]
        }
        return data
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecurityACL.check(json.project,Project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        synchronized (this.getClass()) {
            Command c = new AddCommand(user: currentUser)
            executeCommand(c,null,json)
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(ImageInstance domain, def jsonNewData) {
        SecurityACL.check(domain.container(),READ)
        SecurityACL.check(jsonNewData.project,Project,READ)

        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)
        executeCommand(c,domain,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ImageInstance domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecurityACL.check(domain.container(),READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.baseImage?.filename, domain.project.name]
    }

    def deleteDependentAlgoAnnotation(ImageInstance image,Transaction transaction, Task task = null) {
        AlgoAnnotation.findAllByImage(image).each {
             algoAnnotationService.delete(it,transaction)
        }
    }

    def deleteDependentReviewedAnnotation(ImageInstance image,Transaction transaction, Task task = null) {
        ReviewedAnnotation.findAllByImage(image).each {
            reviewedAnnotationService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentUserAnnotation(ImageInstance image,Transaction transaction, Task task = null) {
        UserAnnotation.findAllByImage(image).each {
            userAnnotationService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentUserPosition(ImageInstance image,Transaction transaction, Task task = null) {
        UserPosition.findAllByImage(image).each {
            it.delete()
        }
    }
}
