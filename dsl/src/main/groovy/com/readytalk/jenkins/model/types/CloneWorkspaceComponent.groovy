package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

/**
 * Enables jobs in a pipeline to use an upstream job's workspace without having to
 * explicitly configure and wire up workspace archiving
 *
 * TODO: Restrict usage to pipeline jobs, since it won't work otherwise
 */
@Fixed
class CloneWorkspaceComponent extends AbstractComponentType {
  String name = 'cloneWorkspace'

  Map<String,?> fields = [
          upstream: null, //required
          criteria: 'Any',
  ]

  Closure dslConfig = { vars ->
    scm {
      cloneWorkspace(vars.upstream, vars.criteria)
    }
  }
}
