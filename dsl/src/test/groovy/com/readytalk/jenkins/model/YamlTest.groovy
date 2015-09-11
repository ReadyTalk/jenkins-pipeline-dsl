package com.readytalk.jenkins.model

import com.readytalk.jenkins.ModelGenerator
import com.readytalk.jenkins.model.pipelines.BasicPipeline
import com.readytalk.jenkins.model.pipelines.SequentialPipeline
import com.readytalk.jenkins.model.types.GitComponent
import com.readytalk.jenkins.model.types.OwnershipComponent
import com.readytalk.jenkins.model.types.ParameterizedComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent
import spock.lang.Ignore

class YamlTest extends ModelSpecification {
  def setup() {
    registry.add(ParameterizedComponent.instance)
    registry.add(TriggerDownstreamComponent.instance)
    registry.add(BasicPipeline.getType())
    registry.add(GitComponent.instance)
    registry.add(SequentialPipeline.getType())
  }

  def "can parse simple yaml config"() {
    when:
    def items = eval(YamlParser.parse("""
- pipeline:
    name: pipetest
    git:
      repo: team/repository
    build:
      type: fakeJob
    deploy:
      type: deployJob
"""))

    then:
    items.find { it.itemName == 'pipetest-build' }.lookupValue('git', 'repo') == 'team/repository'
    items.find { it.itemName == 'pipetest-deploy'} != null
  }

  def "can add components with empty config"() {
    when:
    def items = eval(YamlParser.parse("""
- fakeJob:
    name: componentTest
    parameterized:
    git:
"""))

    then:
    items.find { it.itemName == 'componentTest' }.components.contains(GitComponent.instance)
  }

  def "can add parameterized elements using yaml config"() {
    when:
    String globalBranch = 'not-master'
    String jobBranch = 'master'
    def items = eval(YamlParser.parse(
"""group:
  - parameterized:
      parameters:
        BRANCH: not-master
  - fakeJob:
      name: paramTestOverride
      parameterized:
        parameters:
          BRANCH: master
  - fakeJob:
      name: paramTest
      parameterized:
"""))

    then:
    items.find { it.itemName == 'paramTestOverride' }.lookupValue('parameterized', 'parameters').get('BRANCH') == jobBranch
    items.find { it.itemName == 'paramTest' }.lookupValue('parameterized', 'parameters').get('BRANCH') == globalBranch
  }

  def "can embed groovy code for upstream dsl block in base component"() {
    when:
    def jobs = generate(YamlParser.parse('''
- jerbJob:
    name: a-job
    base:
      dsl: |
        steps {
          shell "echo 'Repo key: ${vars.git.repo}'"
        }
    git:
      repo: team/repo
    parameterized:
'''))

    def xml = jobs.find { it.name == 'a-job' }.getNode()

    then:
    xml.builders.'hudson.tasks.Shell'.command[0].value().contains('team/repo')
  }

  def "can parse sequential pipeline"() {
    when:
    def items = eval(YamlParser.parse("""
- sequential:
    name: list
    job-name:
      type: fakeJob
    another-job:
      type: fakeJob
"""))

    then:
    items.size() == 2
    items.find { it.itemName == 'list-job-name' } != null
    items.find { it.itemName == 'list-another-job' } != null
  }

  //TODO: Not really a yaml test, should move this
  def "can template values"() {
    when:
    types {
      add OwnershipComponent.instance
    }

    def jobs = generate(YamlParser.parse('''
- ownership:
    team: term
    email: !template group.${ownership.team}@example.com
- fakeJob:
    name: fake
    ownership:
'''))

    def xml = jobs.find { it.name == 'fake' }.getNode()

    then:
    xml.publishers.'hudson.tasks.Mailer'.recipients[0].value().contains('group.term@example.com')
  }
}
