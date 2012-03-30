package be.cytomine

import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON
import be.cytomine.test.BasicInstance

import be.cytomine.test.Infos
import be.cytomine.ontology.Term
import be.cytomine.test.HttpClient
import be.cytomine.ontology.Ontology

import be.cytomine.ontology.AnnotationTerm
import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 10/02/11
 * Time: 9:31
 * To change this template use File | Settings | File Templates.
 */
class TermTests extends functionaltestplugin.FunctionalTestCase {


  void testListOntologyTermByOntologyWithCredential() {

    Term term = BasicInstance.createOrGetBasicTerm()

    log.info("get by ontology")
    String URL = Infos.CYTOMINEURL+"api/ontology/"+term.ontology.id+"/term.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response")
    assertEquals(200,code)
    def json = JSON.parse(response)
    assert json instanceof JSONArray

  }

  void testListTermOntologyByOntologyWithOntologyNotExist() {

    log.info("get by ontology not exist")
    String URL = Infos.CYTOMINEURL+"api/ontology/-99/term.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response")
    assertEquals(404,code)
    def json = JSON.parse(response)

  }

  void testListTermOntologyByTermWithCredential() {

    Term term = BasicInstance.createOrGetBasicTerm()

    log.info("get by term")
    String URL = Infos.CYTOMINEURL+"api/term/"+term.id+"/ontology.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response:"+response)
    assertEquals(200,code)
    def json = JSON.parse(response)

  }

  void testListTermOntologyByTermWithTermNotExist() {

    log.info("get by term not exist")
    String URL = Infos.CYTOMINEURL+"api/term/-99/ontology.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response")
    assertEquals(404,code)
    def json = JSON.parse(response)

  }

  void testListTermByImageWithCredential() {

    AnnotationTerm annotationTerm = BasicInstance.createOrGetBasicAnnotationTerm()

    log.info("get by ontology")
    String URL = Infos.CYTOMINEURL+"api/imageinstance/"+annotationTerm.annotation.image.id+"/term.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response")
    assertEquals(200,code)
    def json = JSON.parse(response)
    assert json instanceof JSONArray

  }

  void testListTermByImageWithImageNotExist() {

    log.info("get by ontology not exist")
    String URL = Infos.CYTOMINEURL+"api/image/-99/term.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response")
    assertEquals(404,code)
    def json = JSON.parse(response)

  }

  void testListTermWithCredential() {

    log.info("get term")
    String URL = Infos.CYTOMINEURL+"api/term.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response:"+response)
    assertEquals(200,code)
    def json = JSON.parse(response)
    assert json instanceof JSONArray
  }

  void testListTermWithoutCredential() {

    log.info("get term")
    String URL = Infos.CYTOMINEURL+"api/term.json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.BADLOGIN,Infos.BADPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response")
    assertEquals(401,code)
  }

  void testShowTermWithCredential() {

    log.info("create term")
    Term term =  BasicInstance.createOrGetBasicTerm()

    log.info("get term")
    String URL = Infos.CYTOMINEURL+"api/term/"+ term.id +".json"
    HttpClient client = new HttpClient();
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();

    log.info("check response:"+response)
    assertEquals(200,code)
    def json = JSON.parse(response)
    assert json instanceof JSONObject
  }

  void testAddTermCorrect() {

    log.info("create term")
    def termToAdd = BasicInstance.getBasicTermNotExist()
    String jsonTerm = termToAdd.encodeAsJSON()

    log.info("post term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term.json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.post(jsonTerm)
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    println response
    client.disconnect();

    log.info("check response")
    assertEquals(200,code)
    def json = JSON.parse(response)
    assert json instanceof JSONObject
    int idTerm = json.term.id

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

    log.info("test undo")
    client = new HttpClient()
    URL = Infos.CYTOMINEURL+Infos.UNDOURL +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

    log.info("check if object "+ idTerm +" not exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(404,code)

    log.info("test redo")
    client = new HttpClient()
    URL = Infos.CYTOMINEURL+Infos.REDOURL +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

    //must be done because redo change id
    json = JSON.parse(response)
    assert json instanceof JSONArray
    idTerm = json[0].term.id

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

  }

  void testAddTermWithBadName() {

    log.info("create term")
    def termToAdd = BasicInstance.createOrGetAnotherBasicTerm()
    String jsonTerm = termToAdd.encodeAsJSON()

    log.info("post term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term.json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.post(jsonTerm)
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    println response
    client.disconnect();

    log.info("check response")
    assertEquals(409,code)

  }

  void testEditTermCorrect() {

    String oldName = "Name1"
    String newName = "Name2"

    String oldComment = "Comment1"
    String newComment = "Comment2"

    String oldColor = "000000"
    String newColor = "FFFFFF"

    Ontology oldOntology = BasicInstance.createOrGetBasicOntology()
    Ontology newOntology = BasicInstance.getBasicOntologyNotExist()
    newOntology.save(flush:true)

    def mapOld = ["name":oldName,"comment":oldComment,"color":oldColor,"ontology":oldOntology]
    def mapNew = ["name":newName,"comment":newComment,"color":newColor,"ontology":newOntology]


    /* Create a Name1 term */
    log.info("create term")
    Term termToAdd = BasicInstance.createOrGetBasicTerm()
    termToAdd.name = oldName
    termToAdd.comment = oldComment
    termToAdd.color = oldColor
    termToAdd.ontology = oldOntology
    assert (termToAdd.save(flush:true) != null)

    /* Encode a niew term Name2*/
    Term termToEdit = Term.get(termToAdd.id)
    def jsonTerm = termToEdit.encodeAsJSON()
    def jsonUpdate = JSON.parse(jsonTerm)
    jsonUpdate.name = newName
    jsonUpdate.comment = newComment
    jsonUpdate.color = newColor
    jsonUpdate.ontology = newOntology.id
    jsonTerm = jsonUpdate.encodeAsJSON()

    log.info("put term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term/"+termToEdit.id+".json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.put(jsonTerm)
    int code  = client.getResponseCode()
    String response = client.getResponseData()
    println response
    client.disconnect();

    log.info("check response")
    assertEquals(200,code)
    def json = JSON.parse(response)
    assert json instanceof JSONObject
    int idTerm = json.term.id

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();

    assertEquals(200,code)
    json = JSON.parse(response)
    assert json instanceof JSONObject

    BasicInstance.compareTerm(mapNew,json)

    log.info("test undo")
    client = new HttpClient()
    URL = Infos.CYTOMINEURL+Infos.UNDOURL + ".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();

    assertEquals(200,code)
    json = JSON.parse(response)
    assert json instanceof JSONObject

    BasicInstance.compareTerm(mapOld,json)

    log.info("test redo")
    client = new HttpClient()
    URL = Infos.CYTOMINEURL+Infos.REDOURL + ".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();

    assertEquals(200,code)
    json = JSON.parse(response)
    assert json instanceof JSONObject

    BasicInstance.compareTerm(mapNew,json)


    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();

    assertEquals(200,code)
    json = JSON.parse(response)
    assert json instanceof JSONObject

  }

  void testEditTermWithBadName() {

    /* Create a Name1 term */
    log.info("create term")
    Term term= BasicInstance.getBasicTermNotExist()
    term.save(flush:true)
    Term termToAdd = BasicInstance.createOrGetBasicTerm()

    /* Encode a niew term Name2*/
    Term termToEdit = Term.get(termToAdd.id)
    def jsonTerm = termToEdit.encodeAsJSON()
    def jsonUpdate = JSON.parse(jsonTerm)
    jsonUpdate.name = term.name
    jsonTerm = jsonUpdate.encodeAsJSON()

    log.info("put term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term/"+termToEdit.id+".json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.put(jsonTerm)
    int code  = client.getResponseCode()
    client.disconnect();

    log.info("check response")
    assertEquals(409,code)

  }

  void testDeleteTerm() {

    log.info("create term")
    def termToDelete = BasicInstance.createOrGetBasicTerm()
    String jsonTerm = termToDelete.encodeAsJSON()
    int idTerm = termToDelete.id
    log.info("delete term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term/"+idTerm+".json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.delete()
    int code  = client.getResponseCode()
    client.disconnect();

    log.info("check response")
    assertEquals(200,code)

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    client.disconnect();

    assertEquals(404,code)

    log.info("test undo")
    client = new HttpClient()
    URL = Infos.CYTOMINEURL+Infos.UNDOURL +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.get()
    code  = client.getResponseCode()
    String response = client.getResponseData()
    client.disconnect();
    assertEquals(200,code)

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm  +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    response = client.getResponseData()
    client.disconnect();

    assertEquals(200,code)


    log.info("test redo")
    client = new HttpClient()
    URL = Infos.CYTOMINEURL+Infos.REDOURL +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.get()
    code  = client.getResponseCode()
    client.disconnect();
    assertEquals(200,code)

    log.info("check if object "+ idTerm +" exist in DB")
    client = new HttpClient();
    URL = Infos.CYTOMINEURL+"api/term/"+idTerm +".json"
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD);
    client.get()
    code  = client.getResponseCode()
    client.disconnect();
    assertEquals(404,code)

  }

  void testDeleteTermNotExist() {

    log.info("create term")
    def termToDelete = BasicInstance.createOrGetBasicTerm()
    String jsonTerm = termToDelete.encodeAsJSON()

    log.info("delete term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term/-99.json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.delete()
    int code  = client.getResponseCode()
    client.disconnect();

    log.info("check response")
    assertEquals(404,code)
  }

  void testDeleteTermWithData() {

    log.info("create term")
    def annotTerm = BasicInstance.createOrGetBasicAnnotationTerm()
    def termToDelete = annotTerm.term
    String jsonTerm = termToDelete.encodeAsJSON()

    log.info("delete term:"+jsonTerm.replace("\n",""))
    String URL = Infos.CYTOMINEURL+"api/term/"+ termToDelete.id +".json"
    HttpClient client = new HttpClient()
    client.connect(URL,Infos.GOODLOGIN,Infos.GOODPASSWORD)
    client.delete()
    int code  = client.getResponseCode()
    client.disconnect();

    log.info("check response")
    assertEquals(200,code)
  }

  void testAddTermAnnotationMapping() {

  }

  void testAddTermAnnotationMappingAlreadyExist() {

  }

  void testAddTermNoExistToAnnotationMapping()  {

  }

  void testAddTermToAnnotationNotExistMapping() {

  }

}
