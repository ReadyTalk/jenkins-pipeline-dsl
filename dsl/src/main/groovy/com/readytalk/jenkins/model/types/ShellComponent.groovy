package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType

import com.readytalk.jenkins.model.Fixed

@Fixed
class ShellComponent extends AbstractComponentType {
  String name = 'shell'
  Map<String,?> fields = [
          command: '',   //default priority
          init:    '',   //Run command before anything else
          setup:   '',   //Run before other commands, but after init
          final:   '',   //Run after other commands
  ]

  Map<Integer,Closure> dslBlocks = [
          (10): sh('init'),
          (30): sh('setup'),
          (50): sh('command'),
          (80): sh('final')
  ]

  Closure sh(String field) {
    return { vars ->
      def cmd = vars."${field}"
      if (cmd != '') {
        steps {
          shell(cmd)
        }
      }
    }
  }
}
