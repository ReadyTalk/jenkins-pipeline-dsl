package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.CommonComponent

class ValidatorTest extends ModelSpecification {
  def "validation fails when referencing invalid component name"() {
    when:
    eval {
      jerbJob('bad') {
        unreal 'beep boop'
      }
    }

    then:
    def e = thrown(RuntimeException)
    e.message.contains("'unreal'")
    e.message.contains('does not match')
  }

  def "validation fails if attempting to assign component as property"() {
    when:
    model {
      jerbJob('mostlyBad') {
        gibberish = 'void/null'
      }
    }

    then:
    def e = thrown(RuntimeException)
    e.message.contains("DSL syntax error")
    e.message.contains('gibberish')
  }

  def "validation fails with informative error if referencing field as component"() {
    when:
    registry.add(CommonComponent.instance)

    model {
      jerbJob('mostlyBad') {
        buildHost 'void/null'
      }
    }

    then:
    def e = thrown(RuntimeException)
    e.message.contains "buildHost"
    e.message.contains "common"
  }
}
