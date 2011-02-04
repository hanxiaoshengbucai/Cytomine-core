package be.cytomine.api.project

import grails.converters.*
import be.cytomine.project.Annotation
import be.cytomine.project.Scan
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.geom.Geometry
import be.cytomine.security.User
import be.cytomine.command.Command
import be.cytomine.command.Transaction
import be.cytomine.command.annotation.AddAnnotationCommand
import be.cytomine.command.UndoStack
import be.cytomine.command.annotation.DeleteAnnotationCommand
import be.cytomine.command.annotation.EditAnnotationCommand

class RestAnnotationController {

  def springSecurityService

  def list = {
    println params.idscan
    List<Annotation> data = (params.idscan == null) ? Annotation.list() : (Annotation.findAllByScan(Scan.findById(params.idscan)))
    HashMap jsonMap = getAnnotationsMap(data)
    println data
    withFormat {
      json { render data as JSON }
      xml { render jsonMap as XML}
    }

  }

  //return 404 when not found
  def show = {
    Scan.createOrGetBasicScan()
    println "show:" + params.idannotation
    Annotation data = Annotation.get(params.idannotation)
    if(data!=null)  {
      println "annotation:" + data
      //println Annotation.convertToJSON(data)
      HashMap jsonMap = Annotation.convertToMap(data)
      println "map:" + jsonMap.size()
      // render data as JSON

      withFormat {
        json { render jsonMap as JSON }
        xml { render jsonMap as XML}
      }
    }
    else
    {
      response.status = 404
      render contentType: "application/xml", {
        errors {
          message("Annotation not found with id: " + params.idannotation)
        }
      }
    }
  }

  def add = {

    println "add"
    User currentUser = User.get(3)
    println request.JSON.toString()
    Command addAnnotationCommand = new AddAnnotationCommand(postData : request.JSON.toString())
    Transaction currentTransaction = currentUser.getNextTransaction()
    currentTransaction.addToCommands(addAnnotationCommand)
    def result = addAnnotationCommand.execute()

    if (result.status == 201) {
      addAnnotationCommand.save()
      new UndoStack(command : addAnnotationCommand, user: currentUser).save()
    }

    response.status = result.status
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }


  def delete = {
    println "delete"
    //springSecurityService.principal.id
    User currentUser = User.get(3)
    def postData = ([id : params.idannotation]) as JSON
    println postData.toString()
    Command deleteAnnotationCommand = new DeleteAnnotationCommand(postData : postData.toString())
    Transaction currentTransaction = currentUser.getNextTransaction()
    currentTransaction.addToCommands(deleteAnnotationCommand)
    def result = deleteAnnotationCommand.execute()

    if (result.status == 204) {
      deleteAnnotationCommand.save()
      new UndoStack(command : deleteAnnotationCommand, user: currentUser).save()
    }

    response.status = result.status
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }


  def update = {
    println "update"
    User currentUser = User.get(3)
    println "json=" + request.JSON.toString()
    Command editAnnotationCommand = new EditAnnotationCommand(postData : request.JSON.toString())
    Transaction currentTransaction = currentUser.getNextTransaction()
    currentTransaction.addToCommands(editAnnotationCommand)
    def result = editAnnotationCommand.execute()

    if (result.status == 200) {
      editAnnotationCommand.save()
      new UndoStack(command : editAnnotationCommand, user: currentUser).save()
    }

    response.status = result.status
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }


  def test = {
    println "*** start"
    //  def result = new URL("http://google.com").text
    // println result

    println "1"
    URL url =  new URL("http://localhost:8080/cytomine-web/api/annotation/scan/37.json")
    HttpURLConnection con = (HttpURLConnection)url.openConnection();
    println "2"
    con.setRequestMethod("GET");
    println "3"
    con.connect();
    println "4"
    String reponse=con.getResponseCode();
    println "5"
    println reponse; // test affiche
    println "*** end"
  }





  def addold = {
    println params.location
    Scan scan =  Scan.get(params.idscan)
    if((scan==null)) println "Scan is null"
    else println "Scan is not null"

    Geometry geom = new WKTReader().read(params.location);
    Annotation annotation = new Annotation(name: "toto", location:geom, scan:scan)

    if(annotation.validate())
    {
      annotation.save(flush:true)

      def annotationList = []
      annotationList.add(annotation)
      HashMap jsonMap = getAnnotationsMap(annotationList)

      withFormat {
        json { render jsonMap as JSON }
        xml { render jsonMap as XML}
      }

    }
    else
    {
      println("\n\n\n Errors in account boostrap for ${item.name}!\n\n\n")
      scan.errors.each {
        err -> println err
      }
    }
  }


  def updateold = {
    println params.location
    Annotation annotation =  Annotation.get(params.idannotation)
    if((annotation==null)) println "Annotation is null"
    else println "Annotation is not null"

    Geometry geom = new WKTReader().read(params.location);
    annotation.location = geom

    if(annotation.validate())
    {
      annotation.save(flush:true)

      def annotationList = []
      annotationList.add(annotation)
      HashMap jsonMap = getAnnotationsMap(annotationList)

      withFormat {
        json { render jsonMap as JSON }
        xml { render jsonMap as XML}
      }

    }
    else
    {
      println("\n\n\n Errors in account boostrap for ${item.name}!\n\n\n")
      scan.errors.each {
        err -> println err
      }
    }
  }

  def deleteold = {
    println params.idannotation
    Annotation annotation =  Annotation.get(params.idannotation)
    if((annotation==null)) println "Annotation is null"
    else println "Annotation is not null"

    annotation.delete()

    def annotationList = []
    annotationList.add(annotation)
    HashMap jsonMap = getAnnotationsMap(annotationList)

    withFormat {
      json { render jsonMap as JSON }
      xml { render jsonMap as XML}
    }
  }

  /* Take a List of annotation(s) and return a Map of annotation with only some attribute.
  *  Avoid that the converter go into the geometry object.
  * */
  def getAnnotationsMap(annotationList) {
    if(annotationList==null || annotationList.size()==0)
    {
      println "annotationList==null | annotationList.size==0"
      HashMap jsonMap = new HashMap()
      jsonMap.annotations = []
      jsonMap

    }
    else
    {
      println "annotationList.size!=0"
      HashMap jsonMap = new HashMap()
      jsonMap.annotations = annotationList.collect {ann ->
        return [id: ann.id, name: ann.name, location: ann.location.toString(), scan: ann.scan.id]
      }
      println "jsonMap=" + jsonMap.size()
      jsonMap
    }
  }

  def getAnnotation(ann) {

    println "annotation:"+ ann

    HashMap jsonMap = new HashMap()
    jsonMap.annotation = [id: ann.id, name: ann.name, location: ann.location.toString(), scan: ann.scan.id]

    println "jsonMap=" + jsonMap.size()
    jsonMap

  }

}
