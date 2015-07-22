package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ModelSpecification
import com.readytalk.jenkins.model.YamlParser

class PullRequestTest extends ModelSpecification {
  def setup() {
    types {
      add OwnershipComponent.instance
      add GitComponent.instance
      add PullRequestComponent.instance
      add TriggerDownstreamComponent.instance
      add ParameterizedComponent.instance
    }
  }

  def "Pull request component uses same repository"() {
    when:
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

  def "Pull request component disables active notifications"() {
    when:
    def jobs = generate(YamlParser.parse("""
- ownership:
    team: fakeTeam
    email: fake@example.com
    hipchatRooms: fakeRoom

- fakeJob:
    name: jerb
    pullRequest:
      notifications: false
    git:
    ownership:
    parameterized:
"""))
    def prJob = jobs.find { it.name == 'jerb-pr-builder' }.getNode()
    println "prJob: ${prJob.publishers}"
    println prJob.publishers.'hudson.tasks.Mailer'

    then:
    prJob.publishers.'hudson.tasks.Mailer'.recipients == []
    prJob.publishers.'hudson.plugins.jabber.im.transport.JabberPublisher' == []
  }
}
