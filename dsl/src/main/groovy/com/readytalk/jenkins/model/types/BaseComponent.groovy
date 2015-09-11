package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class BaseComponent extends AbstractComponentType {
  String name = 'base'

  Map<String,?> fields = [
          name: null,         //Item name to be passed into DslDelegate
          type: 'job',        //Upstream type
          jenkins: 'JENKINS', //Jenkins URL - optional
          dsl:  { vars-> },   //Special: upstream dsl escape hatch allowing direct access to upstream dsl
  ]

  Closure dslConfig = { vars ->
    def dslBlock = vars.dsl.clone()
    dslBlock.setDelegate(delegate)
    dslBlock.call(vars)
  }
}
