package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.CommonComponent
import com.readytalk.jenkins.model.types.MatrixComponent

class ComponentTest extends ModelSpecification {
  def "Description field aggregates correctly"() {
    when:
    types {
      add CommonComponent.instance
    }
    def job = eval {
      common {
        description = 'outer'
      }
      basicJob('jerb') {
        common {
          description = 'inner'
        }
      }
    }.find { it.itemName == 'jerb' }
    String desc = job.lookupValue('common', 'description')

    then:
    desc.contains('outer')
    desc.contains('inner')
  }

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
  static class ImplicitFieldComponent extends AnnotatedComponentType {
    String name = 'faker'

    @ComponentField String exampleField = 'hello'

    @ComponentField String[] listExample = ['one', 'two', 'three']
  }

  def "can create components using ComponentField annotations"() {
    when:
    def component = ImplicitFieldComponent.instance
    def fieldMap = component.getFields()
    then:
    fieldMap.exampleField == 'hello'
    fieldMap.listExample == ['one', 'two', 'three']
  }
}
