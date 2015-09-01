package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.ComponentField
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ImplicitFields
import com.readytalk.jenkins.model.meta.JobTypeOverride

@Fixed
class BuildFlowComponent extends AbstractComponentType implements ImplicitFields {
  String name = 'scripted'

  List traits = [
          new JobTypeOverride() {
            String jobType = 'buildFlowJob'
          }
  ]

  @ComponentField String script = ''

  Closure dslConfig = { vars ->
    buildFlow(vars.script)
  }
}
