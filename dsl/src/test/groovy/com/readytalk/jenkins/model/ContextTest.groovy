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
    child.lookup('cAlpha', 'field').get() == 'alphaRootValue'
    child2.lookup('cBeta', 'field').get() == 'betaRootValue'
  }

  def "ContextMap lookup uses most specific value"() {
    when:
    def child = root.createChildContext().bind('cBeta', 'field', 'childValue')
    then:
    child.lookup('cBeta','field').get() == 'childValue'
  }

  def "context lookup can be proxied"() {
    when:
    def proxy = createProxy()
    def alphaProxy = proxy.cAlpha
    def block = { vars -> return vars.field.equals('alphaRootValue') }

    then:
    alphaProxy.cBeta.field == 'betaRootValue'
    alphaProxy.field == 'alphaRootValue'
    proxy.cBeta.field == 'betaRootValue'
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
    proxy.cAlpha.field == 'newAlphaValue'
    betaProxy.field == 'newBetaValue'
    proxy.cDelta.field == 'equalsValue'
  }
}
