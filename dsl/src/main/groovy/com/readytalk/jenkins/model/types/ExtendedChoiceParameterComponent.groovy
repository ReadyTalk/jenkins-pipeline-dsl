package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class ExtendedChoiceParameterComponent extends AbstractComponentType {
  String name = 'scriptedParameters'

  Map<String,?> fields = [
    name: '',
    choicesScript: '',
    defaultsScript: '',
    visibleItemCount: '10',
  ]

  Closure dslConfig = { vars ->
    configure { project ->
      project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / parameterDefinitions / 'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition'(plugin: 'extended-choice-parameter@0.34') {
        name(vars.name)
        description()
        quoteValue(false)
        visibleItemCount(vars.visibleItemCount)
        type(PT_MULTI_SELECT)
        groovyScript(vars.choicesScript)
        bindings()
        defaultGroovyScript(vars.defaultsScript)
        defaultBindings()
        multiSelectDelimiter(",")
      }
    }
  }
}
