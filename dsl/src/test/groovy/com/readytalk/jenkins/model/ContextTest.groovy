package com.readytalk.jenkins.model

class ContextTest extends ModelSpecification {
  ContextMap root

  def setup() {
    root = new ContextMap(null, [:])
    root.bind('cAlpha', 'field', 'alphaRootValue')
    root.bind('cBeta', 'field', 'betaRootValue')
  }

  def createProxy() {
    ProxyDelegate proxy = new ContextProxy(registry: registry, getter: root, setter: root).generate()
    //Ensure registry is filled out
    model {
      cAlpha { field 'alphaValue' }
      cBeta { field 'betaValue' }
      cDelta { }
    }
    return proxy
  }

  def "ContextMap lookup delegates upwards if needed"() {
    when:
    def child = root.createChildContext()
    def child2 = root.createChildContext()
    then:
    child.lookup('cAlpha', 'field').equals('alphaRootValue')
    child2.lookup('cBeta', 'field').equals('betaRootValue')
  }

  def "ContextMap lookup uses most specific value"() {
    when:
    def child = root.createChildContext().bind('cBeta', 'field', 'childValue')
    then:
    child.lookup('cBeta','field').equals('childValue')
  }

  def "context lookup can be proxied"() {
    when:
    def proxy = createProxy()
    def alphaProxy = proxy.cAlpha
    def block = { vars -> return vars.field.equals('alphaRootValue') }

    then:
    alphaProxy.cBeta.field.equals('betaRootValue')
    alphaProxy.field.equals('alphaRootValue')
    proxy.cBeta.field.equals('betaRootValue')
    block.call(alphaProxy)
  }

  def "context binding can be proxied"() {
    when:
    def proxy = createProxy()
    def betaProxy = proxy.cBeta
    proxy.cAlpha.field = 'newAlphaValue'
    def block = {
      cBeta {
        field 'newBetaValue'
      }
      cDelta {
        field = 'equalsValue'
      }
    }
    block.setDelegate(proxy)
    block.resolveStrategy = Closure.DELEGATE_FIRST
    block.call()

    then:
    proxy.cAlpha.field.equals 'newAlphaValue'
    betaProxy.field.equals 'newBetaValue'
    proxy.cDelta.field.equals 'equalsValue'
  }
}
