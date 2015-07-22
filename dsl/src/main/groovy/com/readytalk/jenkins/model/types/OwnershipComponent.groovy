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
          team: '',         //Team name
          email: '',        //Defaults to "rt.TEAM@readytalk.com"
          vcsProject: new TemplateStr('${team}'),   //Defaults to team name
          hipchatRooms: '',
          hipchatLevel: 'FAILURE_AND_FIXED'
  ]

  Closure dslConfig = { vars ->
    def hipchatServer = 'conf.hipchat.com'
    def rooms = StringUtils.asList(vars.hipchatRooms)
    def roomString = rooms.collect { String room ->
      room.contains('@') ? room : "*${room}@${hipchatServer}"
    }.join(' ')
    publishers {
      if(rooms.size() > 0) {
        publishJabber(roomString) {
          strategyName(vars.hipchatLevel)
        }
      }
      if(vars.email != '') {
        mailer(vars.email, true, true)
      }
    }
  }
}
