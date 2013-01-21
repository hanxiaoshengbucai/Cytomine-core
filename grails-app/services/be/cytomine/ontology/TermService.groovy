package be.cytomine.ontology

import be.cytomine.Exception.ConstraintException
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.ModelService
import be.cytomine.SecurityCheck
import be.cytomine.command.AddCommand
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.security.access.prepost.PreAuthorize

class TermService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def annotationTermService
    def algoAnnotationTermService
    def relationTermService
    def domainService
    def userService

    final boolean saveOnUndoRedoStack = true

    /**
     * List all term, Only for admin
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    def list() {
        return Term.list()
    }

    Term read(def id) {
        def term = Term.read(id)
        if (term) {
            SecurityCheck.checkReadAuthorization(term.ontology)
        }
        term
    }

    Term get(def id) {
        def term = Term.get(id)
        if (term) {
            SecurityCheck.checkReadAuthorization(term.ontology)
        }
        term
    }

    @PreAuthorize("#ontology.hasPermission('READ') or hasRole('ROLE_ADMIN')")
    def list(Ontology ontology) {
        return ontology?.leafTerms()
    }

    @PreAuthorize("#project.ontology.hasPermission('READ') or hasRole('ROLE_ADMIN')")
    def list(Project project) {
        return project?.ontology?.terms()
    }

    @PreAuthorize("#annotation.project.hasPermission('READ') or hasRole('ROLE_ADMIN')")
    def list(UserAnnotation annotation, User user) {
        return AnnotationTerm.findAllByUserAndUserAnnotation(user, annotation).collect {it.term.id}
    }

    @PreAuthorize("#term.ontology.hasPermission('READ') or hasRole('ROLE_ADMIN')")
    def statProject(Term term) {
        def projects = Project.findAllByOntology(term.ontology)
        def count = [:]
        def percentage = [:]

        //init list
        projects.each { project ->
            count[project.name] = 0
            percentage[project.name] = 0
        }

        projects.each { project ->
            def annotations = UserAnnotation.createCriteria().list {
                eq("project", project)
                inList("user", userService.listLayers(project))
            }
            annotations.each { annotation ->
                if (annotation.terms().contains(term)) {
                    count[project.name] = count[project.name] + 1;
                }
            }
        }

        //convert data map to list and merge term name and color
        return convertHashToList(count)
    }

    List convertHashToList(HashMap<String, Integer> map) {
        def list = []
        map.each {
            list << ["key": it.key, "value": it.value]
        }
        list
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @param security Security service object (user for right check)
     * @return Response structure (created domain data,..)
     */
    @PreAuthorize("#security.checkOntologyAccess(#json['ontology']) or hasRole('ROLE_ADMIN')")
    def add(def json, SecurityCheck security) {
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new AddCommand(user: currentUser), json)
    }

    /**
     * Update this domain with new data from json
     * @param json JSON with new data
     * @param security Security service object (user for right check)
     * @return Response structure (new domain data, old domain data..)
     */
    @PreAuthorize("#security.checkOntologyAccess()  or hasRole('ROLE_ADMIN')")
    def update(def json, SecurityCheck security) {
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), json)
    }

    /**
     * Update this domain with new data from json
     * @param json JSON with new data
     * @param security Security service object (user for right check)
     * @return Response structure (new domain data, old domain data..)
     */
    @PreAuthorize("#security.checkOntologyAccess()  or hasRole('ROLE_ADMIN')")
    def delete(def json, SecurityCheck security) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        return deleteTerm(json.id, currentUser, true, null)
    }

    /**
     * Delete a term from database
     * This method should delete all data linked with the term: annotation-term, annotation filter, ...
     * @param idTerm Term id to delete
     * @param currentUser User that delete term
     * @param printMessage Flag to tell the client to print a confirmation message
     * @param transaction Transaction that will packed the command
     * @return Response structure
     */
    def deleteTerm(def idTerm, User currentUser, boolean printMessage, Transaction transaction) throws CytomineException {
        log.info "Delete term: " + idTerm
        Term term = Term.read(idTerm)
        if (term) {
            //Delete Annotation-Term before deleting Term
            annotationTermService.deleteAnnotationTermFromAllUser(term, currentUser, transaction)

            //Delete Suggested-Term before deleting Term
            algoAnnotationTermService.deleteAlgoAnnotationTermFromAllUser(term, currentUser, transaction)

            //Delete relation-Term before deleting Term
            relationTermService.deleteRelationTermFromTerm(term, currentUser, transaction)
        }
        //Delete term
        def json = JSON.parse("{id : $idTerm}")
        return executeCommand(new DeleteCommand(user: currentUser, printMessage: printMessage), json)
    }

    /**
     * Delete a term from database, juste delete term and its relation
     * @param idTerm Term id to delete
     * @param currentUser User that delete term
     * @param printMessage Flag to tell the client to print a confirmation message
     * @param transaction Transaction that will packed the command
     * @return Response structure
     */
    def deleteTermRestricted(def idTerm, User currentUser, boolean printMessage, Transaction transaction) throws CytomineException {
        log.info "Delete term: " + idTerm
        Term term = Term.read(idTerm)
        if (term) {
            //Delete relation-Term before deleting Term
            relationTermService.deleteRelationTermFromTerm(term, currentUser, transaction)
        }
        //Delete term
        def json = JSON.parse("{id : $idTerm}")
        return executeCommand(new DeleteCommand(user: currentUser, printMessage: printMessage, transaction: transaction), json)
    }

    /**
     * Create new domain in database
     * @param json JSON data for the new domain
     * @param printMessage Flag to specify if confirmation message must be show in client
     * Usefull when we create a lot of data, just print the root command message
     * @return Response structure (status, object data,...)
     */
    def create(JSONObject json, boolean printMessage) {
        create(Term.createFromDataWithId(json), printMessage)
    }

    /**
     * Create new domain in database
     * @param domain Domain to store
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def create(Term domain, boolean printMessage) {
        //Save new object
        domainService.saveDomain(domain)
        //Build response message
        return responseService.createResponseMessage(domain, [domain.id, domain.name, domain.ontology?.name], printMessage, "Add", domain.getCallBack())
    }

    /**
     * Destroy domain from database
     * @param json JSON with domain data (to retrieve it)
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def destroy(JSONObject json, boolean printMessage) {
        //Get object to delete
        destroy(Term.get(json.id), printMessage)
    }

    /**
     * Destroy domain from database
     * @param domain Domain to remove
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def destroy(Term domain, boolean printMessage) {
        //Build response message
        if (!AlgoAnnotationTerm.findAllByTerm(domain).isEmpty()) throw new ConstraintException("Term " + domain.id + " has suggested term")
        if (!AnnotationTerm.findAllByTerm(domain).isEmpty()) throw new ConstraintException("Term " + domain.id + " has userannotation term")
        def response = responseService.createResponseMessage(domain, [domain.id, domain.name, domain.ontology?.name], printMessage, "Delete", domain.getCallBack())
        //Delete object
        domainService.deleteDomain(domain)
        return response
    }

    /**
     * Edit domain from database
     * @param json domain data in json
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def edit(JSONObject json, boolean printMessage) {
        //Rebuilt previous state of object that was previoulsy edited
        edit(fillDomainWithData(new Term(), json), printMessage)
    }

    /**
     * Edit domain from database
     * @param domain Domain to update
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def edit(Term domain, boolean printMessage) {
        //Build response message
        def response = responseService.createResponseMessage(domain, [domain.id, domain.name, domain.ontology?.name], printMessage, "Edit", domain.getCallBack())
        //Save update
        domainService.saveDomain(domain)
        return response
    }

    /**
     * Create domain from JSON object
     * @param json JSON with new domain info
     * @return new domain
     */
    Term createFromJSON(def json) {
        return Term.createFromData(json)
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     */
    def retrieve(JSONObject json) {
        Term term = Term.get(json.id)
        if (!term) throw new ObjectNotFoundException("Term " + json.id + " not found")
        return term
    }
}
