package com.readytalk.jenkins

import groovy.transform.CompileStatic
import groovyx.net.http.AuthConfig
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException

class JenkinsClient {
  final String prefix
  final @Delegate RESTClient client

  JenkinsClient(String url, String username, String password) {
    this.client = new RESTClient(url)
    this.prefix = ''
    client.headers.put('Authorization', 'Basic ' + "${username}:${password}".toString().bytes.encodeBase64().toString())
  }

  private JenkinsClient(RESTClient client, String prefix) {
    this.client = client
    this.prefix = prefix
  }

  JenkinsClient withPath(String path) {
    return new JenkinsClient(client, path)
  }

  //Can't use content type xml because it uses XmlSlurper under the hood, which is a huge mess
  Node getItemXml(ItemType type, String name) {
    def result
    try {
      result = client.get(
              path:        prefix + "/${type.toString()}/${name}/config.xml",
              contentType: ContentType.TEXT
      )
    } catch (HttpResponseException e) {
      if(e.response.status == 404) {
        return null
      } else {
        throw e
      }
    }

    return new XmlParser().parseText(result.getData().text)
  }

  void createItemXml(ItemType type, String itemName, Node item) {
    client.post(
            path:        prefix + (type.toString() == 'view' ? '/createView' : '/createItem'),
            contentType: ContentType.XML,
            query:       [name: itemName],
            body:        xmlSerialize(item)
    )
  }

  void updateItemXml(ItemType type, String itemName, Node item) {
    client.post(
            path:        prefix + "/${type.toString()}/${itemName}/config.xml",
            contentType: ContentType.XML,
            body:        xmlSerialize(item),
    )
  }

  //Don't use XmlUtil.serialize() because it pukes spurious warnings all over the console
  @CompileStatic
  static String xmlSerialize(Node xml) {
    def result = new StringWriter()
    def printer = new XmlNodePrinter(new PrintWriter(result))
    printer.preserveWhitespace = true
    printer.print(xml)
    return result
  }
}
