package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.TypeDsl
import com.readytalk.jenkins.model.TypeRegistryBundle
import com.readytalk.jenkins.model.TypeRegistryMap

class DefaultJobs implements TypeRegistryBundle {
  static TypeRegistryMap generateTypes(TypeRegistryMap registry) {
    return TypeDsl.evaluate(registry) {
      job('basicJob', 'gradleProject', ['gradle', 'jvmMetrics', 'pullRequest'])

      job('basicJob', 'puppetModule', ['rake', 'puppetMetrics', 'pullRequest'])


      job('gradleProject', 'gradleJSProject', ['regexStatus']) {
        regexStatus.pattern = '(?)^(assertionerror|error|typeerror)'
        common.useXvnc = true
        jvmMetrics {
          findbugs = false
          warnings = false
          checkstyle = false
          jacoco = false
          cobertura = true
        }
      }

      job('fitnesseTestJob', ['base','common','ownership','parameterized','fitnesse']) {
        parameterized.parameters = [
                suite_name: fitnesse.suite,
                tag_specifier: '',
        ]
        common {
          runSchedule = ''
          buildHost = fitnesse.server.replaceAll(/\..+/,"")
          buildName = '#${BUILD_NUMBER}'
        }
      }
    }
  }
}
