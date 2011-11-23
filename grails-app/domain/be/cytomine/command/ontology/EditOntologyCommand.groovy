package be.cytomine.command.ontology

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 */

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.EditCommand
import be.cytomine.command.UndoRedoCommand
import be.cytomine.ontology.Ontology
import grails.converters.JSON

class EditOntologyCommand extends EditCommand implements UndoRedoCommand {

    boolean saveOnUndoRedoStack = true;

    def execute() throws CytomineException {
        log.info "Execute"
        Ontology updatedOntology = Ontology.get(json.id)
        if (!updatedOntology) throw new ObjectNotFoundException("Ontology " + json.id + " was not found")
        return super.validateAndSave(json, updatedOntology, [updatedOntology.id, updatedOntology.name] as Object[])
    }

    def undo() {
        log.info "Undo"
        def ontologyData = JSON.parse(data)
        Ontology ontology = Ontology.findById(ontologyData.previousOntology.id)
        ontology = Ontology.getFromData(ontology, ontologyData.previousOntology)
        ontology.save(flush: true)
        super.createUndoMessage(ontologyData, ontology, [ontology.id, ontology.name] as Object[])
    }

    def redo() {
        log.info "Redo"
        def ontologyData = JSON.parse(data)
        Ontology ontology = Ontology.findById(ontologyData.newOntology.id)
        ontology = Ontology.getFromData(ontology, ontologyData.newOntology)
        ontology.save(flush: true)
        super.createRedoMessage(ontologyData, ontology, [ontology.id, ontology.name] as Object[])
    }

}