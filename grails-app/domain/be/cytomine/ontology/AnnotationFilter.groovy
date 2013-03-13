package be.cytomine.ontology

import be.cytomine.CytomineDomain
import be.cytomine.Exception.CytomineException
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import org.apache.log4j.Logger

/**
 * Annotation filter define a set of filter for annotation listing
 */
class AnnotationFilter extends CytomineDomain implements Serializable {

    String name
    Project project
    User user

    static hasMany = [terms: Term, users: SecUser]

    static constraints = {
        name (nullable : false, blank : false)
        project (nullable: false)
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static AnnotationFilter insertDataIntoDomain(def json, def domain = new AnnotationFilter()) throws CytomineException {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.name = JSONUtils.getJSONAttrStr(json,'name')
        domain.project = JSONUtils.getJSONAttrDomain(json,"project",new Project(),false)
        domain.user = JSONUtils.getJSONAttrDomain(json,"user",new User(),false)
        json.users?.each { userID ->
            SecUser user = SecUser.read(userID)
            if (user) domain.addToUsers(user)
        }
        json.terms?.each { termID ->
            Term term = Term.read(termID)
            if (term) domain.addToTerms(term)
        }
        return domain;
    }

    /**
     * Create callback metadata
     * Callback will be send whith request response when add/update/delete on this send
     * @return Callback for this domain
     */
    def getCallBack() {
        return [annotationFilterID: this?.id]
    }

    /**
     * Define fields available for JSON response
     * This Method is called during application start
     */
    static void registerMarshaller() {
        Logger.getLogger(this).info("Register custom JSON renderer for " + AnnotationFilter.class)
        JSON.registerObjectMarshaller(AnnotationFilter) {
            def returnArray = [:]
            returnArray['class'] = it.class
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['user'] = it.user?.id
            returnArray['project'] = it.project?.id
            returnArray['terms'] = it.terms.collect { it.id }
            returnArray['users'] = it.users.collect { it.id }
            returnArray['created'] = it.created?.time?.toString()
            returnArray['updated'] = it.updated?.time?.toString()
            return returnArray
        }
    }

    /**
     * Return domain user (annotation user, image user...)
     * By default, a domain has no user.
     * You need to override userDomainCreator() in domain class
     * @return Domain user
     */
    public SecUser userDomainCreator() {
        return user;
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container();
    }
}
