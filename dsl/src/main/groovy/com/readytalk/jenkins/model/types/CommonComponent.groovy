package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class CommonComponent extends AbstractComponentType {
  String name = 'common'

  Map<String, ?> fields = [
          runSchedule:      "@weekly",
          historyDays:      '',  //days to keep build history
          historyCount:     30,  //builds to keep if days not set
          description:      '',  //job description
          buildName:        '#${BUILD_NUMBER}-${GIT_BRANCH}',
          buildHost:        '',
          jdkVersion:       '(Default)',
          useXvnc:          false,
          injectPasswords:  false, //if true, injects global passwords into the build
  ]

  Closure dslConfig = { vars->
    int historyDays = vars.historyDays == '' ? -1 : vars.historyDays.toInteger()
    logRotator(historyDays, vars.historyCount)

    description("${vars.description}${vars.description != '' ? '\n' : ''}This job is auto-generated")

    wrappers {
      colorizeOutput('xterm')
      buildName(vars.buildName)
    }

    triggers {
      cron(vars.runSchedule)
    }

    jdk(vars.jdkVersion)

    label(vars.buildHost)

    if(vars.useXvnc) {
      wrappers {
        xvnc {
          useXauthority()
        }
      }
    }

    if(vars.injectPasswords) {
      wrappers {
        maskPasswords()
        injectPasswords()
      }
    }
  }
}
