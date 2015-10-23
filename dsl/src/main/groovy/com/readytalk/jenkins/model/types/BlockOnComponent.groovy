package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.util.StringUtils

@Fixed
class BlockOnComponent extends AbstractComponentType {
  String name = 'blockOn'

  int priority = 40

  Map<String,?> fields = [
          locks:      [], //locks and latches plugin
          jobRegex:   '', //BuildBlockerPlugin
          upstream:   false, //block while upstream jobs running
          downstream: false, //block while downstream jobs running
  ]

  Closure dslConfig = { vars ->
    vars.jobRegex && blockOn(StringUtils.asString(vars.jobRegex,'\n'))
    vars.downstream && blockOnDownstreamProjects()
    vars.upstream && blockOnUpstreamProjects()
    def lockList = StringUtils.asList(vars.locks,',')
    if(lockList.size() > 0) {
      configure { node ->
        def lockBlock = node / 'buildWrappers' / 'hudson.plugins.locksandlatches.LockWrapper' / 'locks'
        lockList.each { String lockName ->
          def lockNode = lockBlock.appendNode('hudson.plugins.locksandlatches.LockWrapper_-LockWaitConfig')
          lockNode / 'name' << lockName
        }
      }
    }
  }
}
