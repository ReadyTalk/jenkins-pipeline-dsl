package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.CloneWorkspaceComponent
import com.readytalk.jenkins.model.types.OwnershipComponent

class TypeDslTest extends ModelSpecification {
  def "local registry overrides over parent"() {
    when:
    def original = registry.lookup('someComponent')
    def dsl = TypeDsl.evaluate(registry) {
      component('someComponent', [key: 'value']) { vars ->  }
    }
    ComponentType dslType = dsl.lookup('someComponent')

    then:
    original != null && !original.equals(dsl.lookup('someComponent'))
    dslType.fields.containsKey('key')
    dslType.fields.get('key').equals('value')
  }

  def "group type always present"() {
    expect:
    registry.lookup('group') == GroupType.instance
    new TypeRegistryMap().lookup('group') == GroupType.instance
    TypeRegistryMap.getDefaultTypes().lookup('group') == GroupType.instance
  }

  def "can define defaults component"() {
    when:
    types {
      add OwnershipComponent.instance
      defaultsComponent('testDefaults', [:]) {
        ownership.team = 'hello'
        ownership.email = 'hello@example.com'
      }
    }

    def jobs = eval(YamlParser.parse("""
- fakeJob:
    name: fake
    testDefaults:
    ownership:
      team: goodbye
"""))

    def job = jobs.find { it.itemName == 'fake' }

    then:
    job.lookup('ownership', 'team') == 'goodbye'
    job.lookup('ownership', 'email') == 'hello@example.com'
  }
}
