package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class VirtualEnvComponent extends AbstractComponentType {
  String name = 'virtualenv'

  Map<String,?> fields = [
    shell: ''
  ]

  Closure dslConfig = { vars ->
    configure { project ->
      project / builders / 'jenkins.plugins.shiningpanda.builders.VirtualenvBuilder'(plugin: 'shiningpanda@0.20') {
        pythonName("System-CPython-2.7")
        home()
        clear(false)
        useDistribute(true)
        systemSitePackages(false)
        nature(shell)
        command("""export PYTHONUNBUFFERED=1 # show output as it comes in
${vars.shell}""")
        ignoreExitCode(false)
      }
    }
  }
}
