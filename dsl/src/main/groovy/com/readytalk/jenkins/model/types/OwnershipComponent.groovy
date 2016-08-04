package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.TemplateStr
import com.readytalk.util.StringUtils

@Fixed
class OwnershipComponent extends AbstractComponentType {
  String name = 'ownership'
  int priority = 95

  Map<String,?> fields = [
          team:          '',
          email:         '',
          vcsProject:    new TemplateStr('${team}'),   //Defaults to team name
          xmppRooms:     '',
          xmppLevel:     'FAILURE_AND_FIXED',
          xmppServer:    '',
          priorityGroup: '', //TODO: What is the default priority if not set?
  ]

  Closure dslConfig = { vars ->
    if(vars.priorityGroup) {
      configure { node ->
        node / 'properties' / 'jenkins.advancedqueue.jobinclusion.strategy.JobInclusionJobProperty' {
          useJobGroup(true)
          jobGroupName(vars.priorityGroup)
        }
      }
    }

    def rooms = StringUtils.asList(vars.xmppRooms)
    def roomString = rooms.collect { String room ->
      room.contains('@') ? room : "*${room}@${vars.xmppServer}"
    }.join(' ')
    publishers {
      if (rooms.size() > 0 && vars.xmppServer != '') {
        publishJabber(roomString) {
          strategyName(vars.xmppLevel)
        }
      }
      if (vars.email) {
        mailer(StringUtils.asString(vars.email, ','), true, true)
      }
    }
  }
}
