package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext
import com.readytalk.jenkins.model.meta.AggregateField
import com.readytalk.util.StringUtils

@Fixed
class ParameterizedComponent extends AbstractComponentType {
  String name = 'parameterized'

  Map<String, ?> fields = [
          parameters:   [:],
          environment:  [:],
          envFile:      '',
          envScript:    '',
          inherit:      true,
          descriptions: [:],
  ]

  List traits = [
          new AggregateField() {
            String aggregateField = 'parameters'
            boolean shouldInherit(ItemSource item) {
              return item.lookupValue(getName(), 'inherit')
            }
          },
          new AggregateField() {
            String aggregateField = 'environment'
            boolean shouldInherit(ItemSource item) {
              return item.lookupValue(getName(), 'inherit')
            }
          }
  ]

  Closure dslConfig = { vars ->
    if(vars.parameters != [:]) {
      parameters {
        vars.parameters.each { String k, v ->
          def desc = vars.descriptions.containsKey(k) ? vars.descriptions.get(k) : ''
          switch (v) {
            case Boolean:
              booleanParam(k, v, desc)
              break
            case List:
              if(v.size() > 0) {
                choiceParam(k, v, desc)
              }
              break
            case Number:
            case String:
            default:
              stringParam(k, StringUtils.asString(v), desc)
          }
        }
      }
    }
    if(vars.environment != [:] && !vars.envFile && !vars.envScript) {
      environmentVariables(vars.environment) {
        if(vars.envFile) propertiesFile(vars.envFile)
        if(vars.envScript) propertiesFile(vars.envScript)
      }
    }
  }
}
