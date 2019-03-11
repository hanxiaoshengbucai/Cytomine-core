package be.cytomine.image

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.Exception.ForbiddenException
import be.cytomine.api.UrlApi
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.server.Storage
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.utils.ModelService
import be.cytomine.utils.SQLUtils
import be.cytomine.utils.Task
import grails.converters.JSON
import groovy.sql.Sql

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class UploadedFileService extends ModelService {

    static transactional = true

    def cytomineService
    def abstractImageService
    def securityACLService
    def dataSource

    def currentDomain() {
        return UploadedFile
    }

    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        def uploadedFiles = UploadedFile.createCriteria().list(sort : "created", order : "desc") {
            isNull("deleted")
        }
        return uploadedFiles
    }

    def list(User user) {
        securityACLService.checkIsSameUser(user, cytomineService.currentUser)
        List<Storage> storages = securityACLService.getStorageList(cytomineService.currentUser)
        def uploadedFiles = UploadedFile.createCriteria().list(sort : "created", order : "desc") {
            eq("user.id", user.id)
            isNull("deleted")
            createAlias("storages", "s")
            'in'("s.id", storages.collect{ it.id })
        }
        return uploadedFiles
    }

    def list(User user, Long parentId, Boolean onlyRoot) {
        securityACLService.checkIsSameUser(user, cytomineService.currentUser)
        List<Storage> storages = securityACLService.getStorageList(cytomineService.currentUser)
        def uploadedFiles = UploadedFile.createCriteria().list(sort : "created", order : "desc") {
            eq("user.id", user.id)
            if(onlyRoot) {
                isNull("parent.id")
            } else if(parentId != null){
                eq("parent.id", parentId)
            }
            isNull("deleted")
            createAlias("storages", "s")
            'in'("s.id", storages.collect{ it.id })
        }
        return uploadedFiles
    }

    def listWithDetails(User user) {
        securityACLService.checkIsSameUser(user, cytomineService.currentUser)

        String request = "SELECT uf.id, " +
                "uf.content_type, " +
                "uf.created, " +
                "uf.filename, " +
                "uf.original_filename, " +
                "uf.size, " +
                "uf.status, " +
                "COUNT(tree.id) as nb_children, " +
                "COALESCE(SUM(tree.size),0)+uf.size as global_size, " +
                "CASE WHEN (uf.status = 1 OR uf.status = 2) THEN ai.id ELSE NULL END as image " +
                "FROM uploaded_file uf " +
                "LEFT JOIN (SELECT * FROM uploaded_file) tree ON (tree.l_tree <@ uf.l_tree AND tree.id != uf.id) " +
                "LEFT JOIN abstract_image ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN uploaded_file_storage as ufs on ufs.uploaded_file_storages_id = uf.id " +
                "LEFT JOIN acl_object_identity as aoi ON aoi.object_id_identity = ufs.storage_id " +
                "LEFT JOIN acl_entry as ae ON ae.acl_object_identity = aoi.id " +
                "LEFT JOIN acl_sid as asi ON asi.id = ae.sid " +
//                "LEFT JOIN (SELECT * FROM uploaded_file) parent ON parent.id = uf.parent_id" +
                "WHERE uf.parent_id is NULL " /*uf.content_type NOT similar to '%zip|ome%' "*/ +
//                "AND (uf.parent_id is null OR parent.content_type similar to '%zip|ome%') " +
                "AND asi.sid = :username " +
                "AND uf.deleted IS NULL " +
                "GROUP BY uf.id, ai.id " +
                "ORDER BY uf.created DESC "

        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request, [username: user.username]) { resultSet ->
            def row = SQLUtils.keysToCamelCase(resultSet.toRowResult())
            row.thumbURL = (row.image) ? UrlApi.getAbstractImageThumbUrl(row.image as Long) : null
            data << row
        }
        sql.close()

        return data
    }

    def listHierarchicalTree(User user, Long rootId){
        UploadedFile root = read(rootId)
        if(!root) {
            throw new ForbiddenException("UploadedFile not found")
        }
        securityACLService.checkAtLeastOne(root, READ)
        String request = "SELECT uf.id, uf.created, uf.original_filename, " +
                "uf.l_tree, uf.parent_id as parent, " +
                "uf.size, uf.status, " +
                "array_agg(ai.id) as images, array_agg(asl.id) as slices, array_agg(cf.id) as companion_files " +
                "FROM uploaded_file uf " +
                "LEFT JOIN abstract_image ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN abstract_slice asl ON asl.uploaded_file_id = uf.id " +
                "LEFT JOIN companion_file cf ON cf.uploaded_file_id = uf.id " +
                "LEFT JOIN uploaded_file_storage as ufs on ufs.uploaded_file_storages_id = uf.id " +
                "LEFT JOIN acl_object_identity as aoi ON aoi.object_id_identity = ufs.storage_id " +
                "LEFT JOIN acl_entry as ae ON ae.acl_object_identity = aoi.id " +
                "LEFT JOIN acl_sid as asi ON asi.id = ae.sid " +
                "WHERE uf.l_tree <@ '" + root.lTree + "'::text::ltree " +
                "AND asi.sid = :username " +
                "AND uf.deleted IS NULL " +
                "GROUP BY uf.id " +
                "ORDER BY uf.l_tree ASC "

        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request, [username: user.username]) { resultSet ->
            def row = SQLUtils.keysToCamelCase(resultSet.toRowResult())
            row.lTree = row.lTree.value
            row.images = row.images.array.findAll { it != null }
            row.slices = row.slices.array.findAll { it != null }
            row.companionFiles = row.companionFiles.array.findAll { it != null }
            row.thumbURL =  null
            if(row.images.size() > 0) {
                row.thumbURL = UrlApi.getAbstractImageThumbUrl(row.images[0] as Long)
            } else if (row.slices.size() > 0) {
                row.thumbURL = UrlApi.getAbstractSliceThumbUrl(row.slices[0] as Long)
            }
            data << row
        }
        sql.close()

        return data
    }

    UploadedFile read(def id) {
        UploadedFile uploadedFile = UploadedFile.read(id)
        if (uploadedFile) {
            securityACLService.checkAtLeastOne(uploadedFile, READ)
        }
        uploadedFile
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        if(currentUser instanceof UserJob) currentUser = ((UserJob)currentUser).user
        securityACLService.checkUser(currentUser)
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(UploadedFile uploadedFile, def jsonNewData, Transaction transaction = null) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        securityACLService.checkAtLeastOne(uploadedFile, WRITE)
        return executeCommand(new EditCommand(user: currentUser, transaction : transaction), uploadedFile,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(UploadedFile domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAtLeastOne(domain, WRITE)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.filename]
    }


    def downloadURI(UploadedFile uploadedFile) {
        securityACLService.checkAtLeastOne(uploadedFile, WRITE)

        return "${uploadedFile.imageServer.url}/image/download?fif=${uploadedFile.path}"
        // "&mimeType=${uploadedFile.image.mimeType}"
    }

    def deleteDependentAbstractImage(UploadedFile uploadedFile, Transaction transaction,Task task=null) {
        //TODO
//        if(uploadedFile.image) abstractImageService.delete(uploadedFile.image,transaction,null,false)
    }

    def deleteDependentAbstractSlice(UploadedFile uploadedFile, Transaction transaction, Task task = null) {
        //TODO
    }

    def deleteDependentCompanionFile(UploadedFile uploadedFile, Transaction transaction, Task task = null) {
        //TODO
    }

    def deleteDependentUploadedFile(UploadedFile uploadedFile, Transaction transaction,Task task=null) {
        taskService.updateTask(task,task? "Delete ${UploadedFile.countByParent(uploadedFile)} uploadedFile parents":"")

        UploadedFile.findAllByParent(uploadedFile).each {
            it.parent = uploadedFile.parent
            this.update(it,JSON.parse(it.encodeAsJSON()), transaction)
        }

        String currentTree = uploadedFile.lTree
        String parentTree = (uploadedFile?.parent?.lTree)?:""

        //1. Set ltree à null de uf
        //2. update tree SET  path = ltree du parent || subpath(path, nlevel('A.C'))  where path <@ 'A.C';
        String request =
                "UPDATE uploaded_file SET l_tree = '' WHERE id= "+uploadedFile.id+";\n" +
                        "UPDATE uploaded_file \n" +
                        "SET l_tree = '"+parentTree+"' || subpath(l_tree, nlevel('"+currentTree+"'))  where l_tree <@ '"+currentTree+"';"

        def sql = new Sql(dataSource)
        sql.execute(request)
        sql.close()
    }
}
