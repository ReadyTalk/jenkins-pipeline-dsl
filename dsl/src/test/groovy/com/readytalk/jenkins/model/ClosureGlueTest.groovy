package com.readytalk.jenkins.model

import com.readytalk.util.ClosureGlue
import spock.lang.Specification

class ClosureGlueTest extends Specification {
  def "calls fallback if function returns null"() {
    when:
    def retNull = { return null }
    def retTrue = { return true }
    def retElseNull = { bool -> return bool ? true : null }
    def retIdentity = { bool -> return bool }
    then:
    ClosureGlue.fallbackIfNull(retNull, retTrue).call()
    ClosureGlue.fallbackIfNull(retTrue, retNull).call()
    ClosureGlue.fallbackIfNull(retElseNull, retIdentity).call(true)
    !(ClosureGlue.fallbackIfNull(retElseNull, retIdentity).call(false))
  }

  def "combines functions preserving delegate"() {
    setup:
    def value = ['Initial']
    def setsAlpha = { val -> val.add('Alpha' + delegate) }
    def setsBeta = { val -> val.add('Beta' + delegate) }
    def combo = ClosureGlue.combinePreservingDelegate(setsAlpha, setsBeta)
    combo.delegate = 'Delegate'

    when:
    combo.call(value)

    then:
    value == ['Initial', 'AlphaDelegate', 'BetaDelegate']
  }

  def "can execute embedded groovy code against context object"() {
    when:
    Map map = [key: 'value']
    def executor = ClosureGlue.executeWith(map, "get('key')")

    then:
    executor.call() == 'value'
  }

  def "can execute embedded groovy code as closure"() {
    when:
    Map map = [key: 'value']
    def executor = ClosureGlue.asClosure("Map vars -> vars.get('key')")

    then:
    executor.call(map) == 'value'
  }
}
