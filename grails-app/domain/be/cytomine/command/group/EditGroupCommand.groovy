package be.cytomine.command.group

import be.cytomine.command.EditCommand
import be.cytomine.command.SimpleCommand
import be.cytomine.security.Group

class EditGroupCommand extends EditCommand implements SimpleCommand {

    def execute() {
        Group updatedGroup = Group.get(json.id)
        return super.validateAndSave(json, updatedGroup, [updatedGroup.id, updatedGroup.name] as Object[])
    }
}
