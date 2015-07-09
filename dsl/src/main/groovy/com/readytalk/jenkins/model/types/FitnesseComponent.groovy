package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class FitnesseComponent extends AbstractComponentType {
  String name = 'fitnesse'

  int priority = 70

  Map<String,?> fields = [
          suite:  null, //test suite (required)
          server: null, //fitnesse hostname (required)
  ]

  Closure dslConfig = { vars ->
    configure { project ->
      project / builders / 'hudson.plugins.fitnesse.FitnesseBuilder'(plugin: 'fitnesse@1.8.1') {
        options {
          entry {
            string('fitnesseTargetPage')
            string('${suite_name}${tag_specifier}')
          }
          entry {
            string("fitnesseTargetIsSuite")
            string("true")
          }
          entry {
            string("fitnesseHost")
            string(vars.server)
          }
          entry {
            string("fitnesseTestTimeout")
            string("64000000")
          }
          entry {
            string("fitnesseHttpTimeout")
            string("64000000")
          }
          entry {
            string("fitnessePortRemote")
            string("8084")
          }
          entry {
            string("fitnesseStart")
            string("false")
          }
          entry {
            string("fitnessePathToXmlResultsOut")
            string("results.xml")
          }
        }
      }
    }

    configure { project ->
      project / publishers / 'hudson.plugins.fitnesse.FitnesseResultsRecorder'(plugin: 'fitness@1.8.1') {
        fitnessePathToXmlResultsIn("results.xml")
      }
    }

    publishers {
      postBuildTask {
        task("Build was aborted", "curl -i -X POST -d 'responder=stoptest' http://${vars.server}:8084/\${suite_name}")
      }
    }
  }
}
