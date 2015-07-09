package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.visitors.ModelPrettyPrinter
import groovy.transform.Immutable

class OrderingTest extends ModelSpecification {
  @Immutable
  static class MockComponent extends AbstractComponentType {
    String name
    int priority
    Closure dslConfig
    Map<String,?> fields
  }

  def "components evaluated before job elements in same scope"() {
    when:
    types {
      component('jobComponent', [field: 'jobDefault']) {vars->}
      component('extComponent', [field: 'jobFirst', field2: 'jobFirst']) {vars->}
      job('oneJob',['jobComponent']) {
        //If component evaluated second, then this default will be wrongly set to twoComponent's default of 'jobFirst'
        //instead of being set to the user-defined value of 'componentFirst'
        jobComponent.field = extComponent.field
      }
      job('twoJob',['jobComponent']) {
        jobComponent.field = extComponent.field2
      }
    }
    def jobs = eval {
      oneJob('theJob')

      oneJob('superJob') {
        twoJob('subJob')
        extComponent {
          field2 'stillComponentFirst'
        }
      }

      extComponent {
        field 'componentFirst'
      }
    }
    ModelPrettyPrinter.printItemList(jobs)

    then:
    jobs.find { it.itemName == 'theJob' }.context.lookup('jobComponent','field') == 'componentFirst'
    jobs.find { it.itemName == 'subJob' }.context.lookup('jobComponent','field') == 'stillComponentFirst'
  }

  def "priority value respected when executing dsl blocks"() {
    when:
    def result = []
    [5,1,4,2,3].collect { p ->
      return new MockComponent("component${p}".toString(), p, {vars-> result.add(p)}, [:])
    }.each { MockComponent fake ->
      registry.types.put(fake.getName(), fake)
    }
    generate {
      jobType('stuffedJob') {
        component1
        component4
        component5
        component2
        component3
      }
    }

    then:
    result == [1,2,3,4,5]
  }
}
