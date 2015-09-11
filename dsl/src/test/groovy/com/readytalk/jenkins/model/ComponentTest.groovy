package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.GitComponent
import com.readytalk.jenkins.model.types.MatrixComponent
import com.readytalk.jenkins.model.types.OwnershipComponent
import com.readytalk.jenkins.model.types.ParameterizedComponent
import com.readytalk.jenkins.model.types.PullRequestComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent
import spock.lang.Ignore

class ComponentTest extends ModelSpecification {
  def "Matrix job axes definition and job type"() {
    when:
    types {
      add(MatrixComponent.instance)
    }

    def definition = {
      fauxJob('jobby') {
        matrix {
          axes = {
            text('axisName', 'value1', 'value2')
          }
        }
      }
    }

    def items = eval(definition)
    def job = generate(definition).find { it.name == 'jobby' }.getNode()
    def axis = job.axes.'hudson.matrix.TextAxis'
    def item = items.find { it.itemName == 'jobby' }

    then:
    item.lookupValue('base', 'type') == 'matrixJob'
    axis.name[0].value() == 'axisName'
    axis.values.string[0].value() == 'value1'
    axis.values.string[1].value() == 'value2'
  }

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
