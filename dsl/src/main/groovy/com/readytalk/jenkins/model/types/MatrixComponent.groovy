package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.meta.ComponentAdapter
import com.readytalk.jenkins.model.meta.JobTypeOverride
import com.readytalk.util.ClosureGlue

@Fixed
class MatrixComponent extends AbstractComponentType {
  String name = 'matrix'

  List<ComponentAdapter> traits = [
          new JobTypeOverride() {
            String jobType = 'matrixJob'
          }
  ]

  //TODO: This component should probably be expanded more
  Map<String,?> fields = [
          axes: '', //Evaluated directly as a dsl string - see jenkins-job-dsl reference
          combinationFilter: ''
  ]

  Closure dsl = { vars ->
    //TODO: We should find a way to abstract the "dsl-string versus map-syntax" pattern out
    if(vars.axes instanceof String) {
      axes(ClosureGlue.asClosure(vars.axes))
    } else if(vars.axes instanceof Closure) {
      axes(vars.axes)
    } else {
      throw new UnsupportedOperationException("Axes defined as dsl string only for now")
    }

    if(vars.combinationFilter != '') {
      combinationFilter(vars.combinationFilter)
    }
  }
}
