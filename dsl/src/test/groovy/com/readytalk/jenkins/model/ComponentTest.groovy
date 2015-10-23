package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.CommonComponent
import com.readytalk.jenkins.model.types.ExtendedEmailComponent
import com.readytalk.jenkins.model.types.MatrixComponent
import com.readytalk.jenkins.model.visitors.ModelPrettyPrinter

class ComponentTest extends ModelSpecification {
  def "Extended email can be applied without error"() {
    setup:
    types {
      add ExtendedEmailComponent.instance
    }

    when:
    def jobConfig = """
- fauxJob:
    name: jerb
    ciEmail:
      recipients: fake@fake.test
      options:
        defaultContent: hello world
      triggers:
        - triggerName: FirstFailure
          recipientList: faux@fake.test
"""
    def job = generate(YamlParser.parse(jobConfig)).find { it.name == 'jerb' }.getNode()

    def extEmail = job.publishers.'hudson.plugins.emailext.ExtendedEmailPublisher'

    then:
    extEmail.recipientList[0].value() == 'fake@fake.test'
    extEmail.configuredTriggers.'hudson.plugins.emailext.plugins.trigger.FirstFailureTrigger'.email.recipientList[0].value() == 'faux@fake.test'
  }

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
    !fieldMap.keySet().contains(null)
  }
}
