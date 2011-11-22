package be.cytomine.command.secusersecrole

import be.cytomine.command.AddCommand
import be.cytomine.security.SecUserSecRole
import be.cytomine.command.SimpleCommand

class AddSecUserSecRoleCommand extends AddCommand implements SimpleCommand {

    def execute() {
        SecUserSecRole userRole = SecUserSecRole.createFromData(json)
        return super.validateAndSave(userRole, ["#ID#", userRole.secUser] as Object[])
    }

}
