package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.GitComponent
import com.readytalk.jenkins.model.types.MatrixComponent
import com.readytalk.jenkins.model.types.OwnershipComponent
import com.readytalk.jenkins.model.types.ParameterizedComponent
import com.readytalk.jenkins.model.types.PullRequestComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent
import spock.lang.Ignore

class ComponentTest extends ModelSpecification {
  def "Can define simple component using type dsl"() {
    when:
    TypeRegistry registry = TypeDsl.evaluate {
      component('organization', [team:'', email:'']) { vars ->
        publishers {
          mailer(vars.email ?: "rt.${vars.team}@readytalk.com", true, true)
        }
      }

      job('exampleJob', ['organization'])
    }

    then:
    registry.lookup('organization') instanceof ComponentType
    noExceptionThrown()
  }

  @Fixed
  static class ImplicitFieldComponent implements ImplicitFields {
    @ComponentField String exampleField = 'hello'

    @ComponentField String[] listExample = ['one', 'two', 'three']
  }

  def "can create components using ComponentField trait"() {
    when:
    def component = ImplicitFieldComponent.instance
    def fieldMap = component.getFields()
    then:
    fieldMap.exampleField == 'hello'
    fieldMap.listExample == ['one', 'two', 'three']
  }
}
