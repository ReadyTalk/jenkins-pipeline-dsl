package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class RegexStatusComponent extends AbstractComponentType {
  String name = 'regexStatus'
  //Try to ensure build isn't prematurely reported as success/failure
  int priority = 40

  Map<String,?> fields = [
          pattern: '',
          file: '',
          checkConsole: true,
          failOnMatch: true
  ]

  Closure dslConfig = { vars->
    publishers {
      if (vars.pattern != '') {
        textFinder(vars.pattern, vars.file, vars.checkConsole, !(vars.failOnMatch))
      }
    }
  }
}
