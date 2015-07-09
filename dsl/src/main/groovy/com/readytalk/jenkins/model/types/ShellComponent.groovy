package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ProxyDelegate

@Fixed
class ShellComponent extends AbstractComponentType {
  String name = 'shell'
  Map<String,?> fields = [
          command: '',   //Actual shell command
          enabled: true, //TODO: allow conditional dsl block here?
  ]

  Closure dslConfig = { ProxyDelegate vars ->
    if(vars.command != '' || !(vars.enabled)) {
      steps {
        shell(vars.command)
      }
    }
  }
}
