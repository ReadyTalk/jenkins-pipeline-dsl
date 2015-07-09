package com.readytalk.jenkins.model

import spock.lang.Specification

class ProxyTest extends Specification {
  def "Can create plain getter proxy"() {
    when:
    def map = [alpha: 'one', beta: 'two']
    def proxy = new ProxyDelegate([get: map.&get] as PropertyProxy)

    then:
    proxy.alpha.equals('one')
    proxy.beta.equals('two')
  }

  def "Can create proxy with setter"() {
    when:
    def map = [alpha: 'one', beta: 'two', omega: [field: '']]
    def proxy = new ProxyDelegate([get: map.&get, set: map.&put] as PropertyProxy)
    proxy.alpha = 'pepper'
    proxy.beta 'salt'
    proxy.omega {
      put('field', 'value')
    }

    then:
    proxy.alpha.equals('pepper')
    proxy.beta.equals('salt')
    proxy.omega.field.equals('value')
  }

  def "Can create delegating getter proxy"() {
    when:
    def cars
    cars = { ...args ->
      println "Train cars left: ${args}"
      if(args.size()==0) return "arrived"
      return new ProxyDelegate([get: { String name ->
          println "first: ${args.first()}, requested: ${name}"
          args.first().equals(name) ? cars.call(*(args.drop(1))) : null
        }] as PropertyProxy, { String name, PropertyProxy px ->
          println "Fallback for ${name}"
          return "derailed"
        }
      )
    }

    def train = cars 'alpha', 'beta', 'delta', 'gamma'

    then:
    train.alpha.beta.delta.gamma.equals('arrived')
    train.alpha.beta.oops.equals('derailed')
  }

  def "Can set closure type properties using direct assignment"() {
    when:
    Map<String,Closure> map = [:]
    def proxy = new ProxyDelegate([get: map.&get, set: map.&put] as PropertyProxy)
    proxy.with {
      alpha = { 'wrapped' }
    }

    then:
    map.get('alpha').call() == 'wrapped'
  }
}
