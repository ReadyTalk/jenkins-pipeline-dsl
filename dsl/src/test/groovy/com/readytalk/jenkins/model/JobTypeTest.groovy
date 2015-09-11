package com.readytalk.jenkins.model

class JobTypeTest extends ModelSpecification {
  def "can define simple job type using type dsl"() {
    when:
    types {
      job('someJob', ['aComponent'])
    }

    then:
    registry.lookup('someJob') instanceof JobType
    noExceptionThrown()
  }

  def "static job defaults override component defaults"() {
    when:
    types {
      job('someJob', ['aComponent']) {
        aComponent.field = 'jobDefault'
      }
    }

    def list = eval {
      someJob('hello-world')
    }

    then:
    registry.lookup('aComponent').fields.field.equals('defaultValue')
    list.find { ItemSource source ->
      source.itemName.equals('hello-world')
    }.context.lookup('aComponent', 'field').get() == 'jobDefault'
  }

  def "components can be dynamically added to jobs"() {
    when:
    def flag = 'wrong'

    types {
      job('blankJob', [])
      component('lambdaComponent', [flag: 'wrong']) { vars ->
        flag = vars.field
      }
    }

    generate {
      blankJob('blank') {
        lambdaComponent {
          field 'correct'
        }
      }
    }

    then:
    flag.equals('correct')
  }

  def "job defaults can append values"() {
    when:
    types {
      job('appender', ['someComponent']) {
        someComponent.field = someComponent.field + "Appended"
      }
    }

    def job = eval {
      appender('append-job')
    }.find { it.itemName == 'append-job' }

    then:
    job.context.lookup('someComponent', 'field').get() == 'defaultValueAppended'
  }
}
