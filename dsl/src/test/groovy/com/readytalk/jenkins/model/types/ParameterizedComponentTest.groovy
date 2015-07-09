package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ModelSpecification
import com.readytalk.jenkins.model.YamlParser

class ParameterizedComponentTest extends ModelSpecification {
  def setup() {
    types {
      add ParameterizedComponent.instance
      add CommonComponent.instance
      job('simpleJob', ['common', 'parameterized']) {
        parameterized.parameters = [JOB: 'job']
      }
    }
  }

  def "Parameterized component allows inheritance of parameters"() {
    when:
    def jobs = eval {
      parameterized {
        parameters = [
          param: 'paramDefault'
        ]
      }
      simpleJob('parameterizedJob') {
        parameterized {
          parameters = [
            another: 'meToo'
          ]
        }
      }
      simpleJob('noInheritJob') {
        parameterized {
          inherit false
          parameters = [
            cheese: 'isAlone'
          ]
        }
      }
    }
    def inheritTrue = jobs.find { it.itemName == 'parameterizedJob' }.context.lookup('parameterized', 'parameters')
    def inheritFalse = jobs.find { it.itemName == 'noInheritJob' }.context.lookup('parameterized', 'parameters')

    then:
    inheritTrue.JOB == 'job'
    inheritTrue.param == 'paramDefault'
    inheritTrue.another == 'meToo'

    inheritFalse.cheese == 'isAlone'
  }

  def "allows implicit choice parameters"() {
    when:
    def jobs = generate {
      fakeJob('faux') {
        parameterized {
          parameters = [
                  BRANCH: ['develop', 'master']
          ]
        }
      }
    }
    def xml = jobs.find { it.name == 'faux' }.getNode()
    def definitions = xml.properties.'hudson.model.ParametersDefinitionProperty'.'parameterDefinitions'
    def choices = definitions.'hudson.model.ChoiceParameterDefinition'.choices.a

    then:
    definitions.'hudson.model.ChoiceParameterDefinition'.name[0].value() == 'BRANCH'
    choices.string[0].value() == 'develop'
    choices.string[1].value() == 'master'
  }

  def "expands templated parameters properly"() {
    when:
    types {
      add OwnershipComponent.instance
    }
    def jobs = generate(YamlParser.parse("""
- parameterized:
    parameters:
      EMAIL_DOMAIN: !template \${base.name}.example.com

- fakeJob:
    name: fake
    ownership:
      email: !template ci@\${parameterized.parameters.EMAIL_DOMAIN}
"""))

    def job = jobs.find { it.name == 'fake' }.getNode()
    println "JOB: ${job.publishers.'hudson.tasks.Mailer'}"

    then:
    job.publishers.'hudson.tasks.Mailer'.recipients[0].value() == 'ci@fake.example.com'

  }

  def "deep nesting still enables inherited parameters"() {
    when:
    def jobs = eval(YamlParser.parse("""
- parameterized:
    parameters:
      ALPHA: one
- group:
  - parameterized:
      parameters:
        BETA: two
  - group:
    - parameterized:
        parameters:
          DELTA: three
    - fakeJob:
        name: fake
        parameterized:
          parameters:
            GAMMA: four
"""))

    println "JOBS: ${jobs}"
    def job = jobs.find { it.itemName == 'fake' }
    def params = job.lookup('parameterized', 'parameters')

    then:
    params.ALPHA == 'one'
    params.BETA == 'two'
    params.DELTA == 'three'
    params.GAMMA == 'four'
  }

}
