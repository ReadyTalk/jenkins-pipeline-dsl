package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.TemplateStr
import org.apache.commons.lang.NotImplementedException

@Fixed
class PipelineSettings extends AbstractComponentType {
  static final String workflowType = 'workflowJob'
  final String name = 'pipelineSettings'

  Map<String,?> fields = [
          script: '',
          useJenkinsfile: false,
          groovySandbox: false,
  ]

  Closure dslConfig = { vars ->
    definition {
      cps {
        sandbox(vars.groovySandbox)
        if(vars.useJenkinsfile.toBoolean()) {
          //TODO: Not yet supported due to git config being weird with single-branch Jenkinsfile projects
          throw new NotImplementedException("Support for non-multibranch Jenkinsfile not yet implemented!")
//          script(readFileFromWorkspace(vars.source))
        } else {
          script(vars.script)
        }
      }
    }
  }
}
