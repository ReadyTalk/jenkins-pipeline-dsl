package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.meta.AggregateField
import com.readytalk.jenkins.model.meta.ComponentAdapter
import com.readytalk.util.StringUtils

@Fixed
class CommonComponent extends AbstractComponentType {
  String name = 'common'

  List<ComponentAdapter> traits = [
          new AggregateField() {
            String aggregateField = 'description'

            boolean shouldInherit(ItemSource item) {
              return item.lookupValue(name, 'inheritDescription')
            }
          }
  ]

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
          concurrentBuild:  false,
          inheritDescription: true,
          quietPeriod:      0, //Seconds to wait before actually starting the build
          timestamps:       true,
  ]

  Closure dslConfig = { vars->
    concurrentBuild(vars.concurrentBuild)

    int historyDays = vars.historyDays == '' ? -1 : vars.historyDays.toInteger()
    logRotator(historyDays, vars.historyCount)

    description("${vars.description}\nThis job is auto-generated: all changes except disabling may be overwritten.")

    if(vars.quietPeriod) {
      quietPeriod(vars.quietPeriod)
    }

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
          useXauthority(true)
        }
      }
    }

    if(vars.injectPasswords) {
      wrappers {
        maskPasswords()
        injectPasswords()
      }
    }

    if(vars.timestamps) {
      wrappers {
        timestamps()
      }
    }
  }
}
