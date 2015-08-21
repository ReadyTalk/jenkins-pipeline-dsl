package com.readytalk.jenkins.model

import com.readytalk.util.ClosureGlue
import javaposse.jobdsl.Run
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

  static interface MockComposableInterface {
    Integer mAdd(Integer a)
    Integer mMult(Integer a)
  }

  static interface MockNonComposableInterface {
    Integer sAdd(Integer a, Integer b)
    Integer sMult(Integer a, Integer b)
  }

  def "can fold monadic interfaces together"() {
    when:
    List actions = [
            [mAdd: {it}, mMult: {it}],
            [mAdd: {it+1}, mMult: {it*2}],
            [mAdd: {it+1}, mMult: {it*2}]
    ].collect { it.asType(MockComposableInterface)}
    MockComposableInterface result =
            ClosureGlue.monadicFold(MockComposableInterface.class, actions)

    then:
    result.mAdd(5) == 7
    result.mMult(2) == 8
  }

  def "refuses to fold non-monadic interfaces"() {
    when:
    List actions = [
            [mAdd: {it+it}, mMult: {it*it}],
            [mAdd: {it+it}, mMult: {it*it}],
    ].collect { it.asType(MockNonComposableInterface)}
    ClosureGlue.monadicFold(MockNonComposableInterface.class, actions)

    then:
    AssertionError e = thrown()
    e.message.contains("ClosureGlue.monadicFold") && e.message.contains('MockNonComposableInterface')
  }
}
