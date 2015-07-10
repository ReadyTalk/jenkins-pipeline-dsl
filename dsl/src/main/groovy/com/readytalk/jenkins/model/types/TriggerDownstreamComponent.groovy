package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.util.StringUtils

@Fixed
class TriggerDownstreamComponent extends AbstractComponentType implements AggregateField {
  String name = 'triggerDownstream'
  //Downstream trigger should the last thing a project does
  int priority = 100

  String aggregateField = 'parameters'

  boolean shouldInherit(ItemSource item) {
    item.lookup(this.getName(), 'inheritParameters')
  }

  //TODO: We really need a way to expose endpoints between jobs besides the pipeline constructs
  //TODO: E.g. downstream job can depend on things in the upstream job via the dsl
  Map<String,?> fields = [
          jobs: '',
          triggerOn: 'SUCCESS',
          sameParameters: true,
          sameCommit: false,
          parameters: [:],
          parameterFile: '',      //Can't default to jenkins.properties, as missing file will remove parameters
          scriptedParameters: '',
          sameNode: false,
          manual: false,
          inheritParameters: true,
          copyWorkspace: false, //can be used without triggering anything, e.g. triggerDownstream { copyWorkspace true }
  ]

  Closure dslConfig = { vars ->
    if(vars.jobs != '') {
      String jobString = StringUtils.asString(vars.jobs, ',')
      Map params = vars.parameters
      if(vars.scriptedParameters != '') {
        steps {
          shell(vars.scriptedParameters)
          environmentVariables {
            propertiesFile(vars.parameterFile)
          }
        }
      }

      publishers {
        def config = {
          if(vars.sameParameters) currentBuild()
          if(vars.sameCommit) gitRevision()
          if(vars.parameters != [:]) predefinedProps(vars.parameters)
          if(vars.sameNode) {
            vars.manual ? predefinedProp('JENKINS_NODE','${NODE_NAME') : sameNode()
          }
          if(vars.parameterFile != '') {
            propertiesFile(vars.parameterFile)
          }
        }
        if(vars.copyWorkspace) {
          publishCloneWorkspace('**', '', 'Any', 'TAR', true)
        }
        if(vars.manual) {
          buildPipelineTrigger(jobString) {
            parameters(config)
          }
        } else {
          downstreamParameterized {
            trigger(jobString, vars.triggerOn, true, [:], config)
          }
        }
      }
    }
  }
}
