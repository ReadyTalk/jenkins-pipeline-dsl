package com.readytalk.jenkins.model

import com.readytalk.jenkins.ItemType
import com.readytalk.jenkins.ModelGenerator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ModelGeneratorTest extends Specification {
  @Rule TemporaryFolder tempDir
  ModelGenerator modelGen = new ModelGenerator()
  Closure fromScript = modelGen.&generateItems << modelGen.&generateFromScript

  def "can parse string as model dsl script"() {
    when:
    def items = (fromScript("""
model {
  basicJob('hello-world')
}""")).get(ItemType.job)
    items.each { k, v -> println "${k}: ${v.getClass()}"}

    then:
    items.any { k, v -> k == 'hello-world' }
  }

  def "can parse file as model dsl script"() {
    setup:
    File dslScript = tempDir.newFile('dsl.groovy')

    when:
    dslScript << """model {
  basicJob('hello-world')
}"""
    println "Contents: ${dslScript.text}"
    def items = (fromScript(dslScript)).get(ItemType.job)

    then:
    items.any { k, v -> k == 'hello-world' }
  }

}
