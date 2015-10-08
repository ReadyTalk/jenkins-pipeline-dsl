package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.ComponentField
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ImplicitFields

@Fixed
class ExtendedEmailComponent extends AbstractComponentType implements ImplicitFields {
  //TODO: Should probably rename this
  String name = 'ciEmail'

  //Needs to happen near the end of the build to avoid emailing too soon
  int priority = 89

  @ComponentField String recipients = ''
  @ComponentField Map options = [:]
  @ComponentField Map triggers = [:]
  @ComponentField Map triggerDefaults = [
          subject: '$PROJECT_DEFAULT_SUBJECT',
          body: '$PROJECT_DEFAULT_BODY',
          sendToDevelopers: true,
          sendToRecipientList: true,
          contentType: 'project',
          replyTo: ''
  ]

  Closure dslConfig = { vars ->
    //Must delay until after baseline xml structures created
    def delayedConfigurations = []

    def customTrigger = { Map options ->
      Map combinedOptions = vars.triggerDefaults + options
      trigger(combinedOptions)
      delayedConfigurations << { node ->
        def triggerContext = node / 'publishers' / 'hudson.plugins.emailext.ExtendedEmailPublisher' / 'configuredTriggers' / "hudson.plugins.emailext.plugins.trigger.${combinedOptions.triggerName}Trigger" / 'email'
        combinedOptions.each { arg, val ->
          if(arg != 'triggerName') {
            triggerContext / arg << val
          }
        }
      }
    }

    publishers {
      extendedEmail(vars.recipients) {
        customTrigger.setDelegate(getDelegate())
        vars.triggers.each { Map params ->
          if(params.triggerName instanceof List) {
            params.triggerName.each { String id ->
              customTrigger(params + [triggerName: id])
            }
          } else {
            customTrigger(params)
          }
        }
      }
    }

    configure { node ->
      delayedConfigurations.each { it.call(node) }
      def emailContext = node / 'publishers' / 'hudson.plugins.emailext.ExtendedEmailPublisher'
      vars.options.each { key, value ->
        emailContext / key << value
      }
    }
  }
}
