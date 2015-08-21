package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.TypeDsl
import com.readytalk.jenkins.model.TypeRegistryBundle
import com.readytalk.jenkins.model.TypeRegistryMap
import org.reflections.Reflections

class DefaultComponents implements TypeRegistryBundle {
  static TypeRegistryMap generateTypes(TypeRegistryMap registry) {
    return TypeDsl.evaluate(registry) {
      component('gradle', [tasks: 'ci', flags: '']) { vars ->
        steps {
          gradle(vars.tasks, vars.flags)
        }
      }

      component('jvmMetrics', [checkstyle: true, findbugs: true, warnings: true, junit: true, jacoco: true, cobertura: false]) { vars ->
        publishers {
          if (vars.checkstyle) checkstyle('**/reports/checkstyle/main.xml')
          if (vars.findbugs) findbugs('**/reports/findbugs/main.xml', true)
          if (vars.warnings) warnings(['Java Compiler (javac)'])
          if (vars.junit) archiveJunit('**/test-results/**/*.xml')
          if (vars.jacoco) {
            jacocoCodeCoverage {
              execPattern "**/**.exec"
              classPattern "**/classes/main"
            }
          }
          if (vars.cobertura) {
            cobertura("**/reports/coverage/**/*.xml")
          }
        }
      }

      component('timeout', [seconds: 0]) { vars ->
        wrappers {
          timeout {
            if(vars.seconds == 0) {
              likelyStuck()
            } else {
              noActivity(vars.seconds)
            }
            abortBuild()
          }
        }
      }
    }
  }
}
