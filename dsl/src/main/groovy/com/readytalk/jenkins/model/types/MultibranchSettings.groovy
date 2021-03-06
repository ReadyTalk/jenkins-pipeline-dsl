package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class MultibranchSettings extends AbstractComponentType {
  final String name = 'multibranchSettings'

  Map<String,?> fields = [
          primaryBranch:  'master', //If blank, build all branches
          remote:         null, //Repository URL (REQUIRED)
          historyDays:    '',  //days to keep build history
          historyCount:   45,  //builds to keep if days not set
          dsl:         {},
  ]

  Closure dslConfig = { vars ->
    branchSources {
      git {
        remote(vars.remote)
        Closure dslBlock = vars.dsl.clone()
        dslBlock.setDelegate(getDelegate())
        dslBlock.call(vars)
      }
    }

    triggers {
      periodic(720)
    }

    orphanedItemStrategy {
      discardOldItems {
        if(vars.historyDays) daysToKeep(vars.historyDays)
        if(vars.historyCount) numToKeep(vars.historyCount)
      }
    }

    //Don't auto-trigger non-master branches from SCM by default
    if(vars.primaryBranch) {
      configure { node ->
        node / 'sources' / 'data' / 'jenkins.branch.BranchSource' / strategy(class: 'jenkins.branch.NamedExceptionsBranchPropertyStrategy') {
          defaultProperties(class: 'java.util.Arrays$ArrayList') {
            a(class: 'jenkins.branch.BranchProperty-array') {
              'jenkins.branch.NoTriggerBranchProperty'()
            }
          }
          namedExceptions(class: 'java.util.Arrays$ArrayList') {
            a(class: 'jenkins.branch.NamedExceptionsBranchPropertyStrategy$Named-array') {
              'jenkins.branch.NamedExceptionsBranchPropertyStrategy_-Named' {
                props(class: 'empty-list')
                name(vars.primaryBranch)
              }
            }
          }
        }
      }
    }
  }
}
