package be.cytomine.processing.job

import be.cytomine.processing.Job
import be.cytomine.security.UserJob

class HelloWorldJobService extends AbstractJobService {

    static transactional = true
    def cytomineService
    def commandService
    def domainService

    def jobParameterService

    def init(Job job, UserJob userJob) {
        log.info "Do nothing..."
    }

    def execute(Job job) {
        //get job params
        log.info "Hello world"
    }


    @Override
    double computeRate(Job job) {
        return -2;
    }
}
