package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.GitComponent
import com.readytalk.jenkins.model.types.OwnershipComponent
import com.readytalk.jenkins.model.types.ParameterizedComponent
import com.readytalk.jenkins.model.types.PullRequestComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent

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

  def "Ownership component sets email publishing"() {
    when:
    types {
      add OwnershipComponent.instance
    }
    def job = generate {
      fakeJob('aJob') {
        ownership {
          team 'fake'
        }
      }
    }.find { it.name == 'aJob' }.getNode()

    then:
    job.publishers.'hudson.tasks.Mailer'.recipients[0].value() == 'rt.fake@readytalk.com'
  }

  def "Pull request component uses same repository"() {
    when:
    types {
      add OwnershipComponent.instance
      add GitComponent.instance
      add PullRequestComponent.instance
      add TriggerDownstreamComponent.instance
      add ParameterizedComponent.instance
    }
    def jobs = generate(YamlParser.parse("""
- fakeJob:
    name: jerb
    ownership:
      team: fake
    pullRequest:
    parameterized:
    git:
"""))

    def prJob = jobs.find { it.name == 'jerb-pr-builder' }.getNode()
    def job = jobs.find { it.name == 'jerb' }.getNode()

    then:
    job.scm.userRemoteConfigs.'hudson.plugins.git.UserRemoteConfig'.url[0].value() ==
            prJob.scm.userRemoteConfigs.'hudson.plugins.git.UserRemoteConfig'.url[0].value()
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
