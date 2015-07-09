package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.util.StringUtils

@Fixed
class ParameterizedComponent extends AbstractComponentType implements AggregateField {
  String name = 'parameterized'

  Map<String, ?> fields = [
          parameters: [:],
          inherit: true
  ]

  String aggregateField = 'parameters'

  boolean shouldInherit(ItemSource item) {
    return item.lookup(this.getName(), 'inherit')
  }

  Closure dslConfig = { vars ->
    if(vars.parameters != [:]) {
      parameters {
        vars.parameters.each { String k, v ->
          switch (v) {
            case Boolean:
              booleanParam(k, v)
              break
            case List:
              if(v.size() > 0) {
                choiceParam(k, v)
              }
              break
            case Number:
            case String:
            default:
              stringParam(k, StringUtils.asString(v))
          }
        }
      }
    }
  }
}
