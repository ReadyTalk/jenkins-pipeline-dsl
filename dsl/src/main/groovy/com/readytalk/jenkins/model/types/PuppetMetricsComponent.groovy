package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class PuppetMetricsComponent extends AbstractComponentType {
  String name = 'puppetMetrics'

  Map<String,?> fields = [
    tasks: 'ci'
  ]

  Closure dslConfig = { vars->
    publishers {
      warnings(["Puppet-Lint"])
    }
  }
}
