package com.readytalk.jenkins.model

import com.readytalk.jenkins.ModelGenerator
import com.readytalk.jenkins.model.visitors.ModelEvaluator
import spock.lang.Ignore

class ModelParserTest extends ModelSpecification {
  def "parses simple components from root"() {
    when:
    def model = model {
      cAlpha {
        field 'alpha'
      }

      cBeta {
        field 'beta'
      }
    }

    ComponentModelElement alpha = model.elements.find { it.type.equals(registry.lookup('cAlpha')) }
    ComponentModelElement beta = model.elements.find { it.type.name.equals('cBeta') }

    then:
    beta != null
    alpha != null
    alpha.fields.field.equals('alpha')
    beta.fields.field.equals('beta')
  }

  def "parses empty job"() {
    when:
    def model = model {
      simpleJob('some random job')
    }

    then:
    ItemModelElement job = model.elements.find { it.type.equals(registry.lookup('simpleJob')) }
    job.name.equals('some random job')
  }

  def "parses job with component defaults"() {
    setup:
    def model = model {
      simpleJob('anotherJob') {
        someComponent {
          field 'jobValue'
        }
      }
    }

    when:
    def job = model.elements.find { it.type.equals(registry.lookup('simpleJob')) }
    def component = job.elements.find { it.type.equals(registry.lookup('someComponent')) }
    String field = component.fields.get('field')

    then:
    job.name.equals('anotherJob')
    component.type.name.equals('someComponent')
    field.equals('jobValue')
  }

  def "parses list values from components"() {
    setup:
    def model = model {
      listComponent {
        field 'one', 'two', 'three'
      }
    }

    when:
    def field = model.elements.find { it.type.equals(registry.lookup('listComponent'))}.fields.get('field')

    then:
    field == ['one', 'two', 'three']
  }

  def "outer scope is correctly restored when returning from inner scope"() {
    when:
    def items = eval {
      fauxComponent {
        field 'outer'
      }

      Jobber('jobOne') {
        fauxComponent {
          field 'inner'
        }
      }

      Jobber('jobTwo')
    }

    then:
    items.find { it.itemName == 'jobOne' }.lookup('fauxComponent', 'field') == 'inner'
    items.find { it.itemName == 'jobTwo' }.lookup('fauxComponent', 'field') == 'outer'
  }
}
