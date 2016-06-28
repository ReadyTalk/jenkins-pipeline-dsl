package com.readytalk.jenkins

import groovy.util.logging.Slf4j
import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.Difference
import org.custommonkey.xmlunit.DifferenceListener
import org.custommonkey.xmlunit.XMLUnit
import org.fusesource.jansi.AnsiRenderer

/**
 * Process an item's xml tree and return the result (e.g. xml file object, http result, etc)
 *
 * @type 'job' or 'view' - folders not strictly supported yet
 * @name name of the item in Jenkins
 * @xml  groovy.util.Node DOM object
 */
interface JenkinsXmlAction {
  Map.Entry<String,?> xmlAction(String path, ItemType type, String name, Node xml)
}

enum ItemType {
  job, view
}

//static collection of predefined xml actions
class JenkinsActions {
  static enum UpdateResult {
    created, unchanged, changed, unknown
  }

  static String folderUriPath(String path) {
    def folder = path.replaceFirst('^/', '')
    if(folder == '') {
      return ''
    } else {
      return "/job/${folder.replaceAll('/', '/job/')}"
    }
  }

  static JenkinsXmlAction jenkinsXmlString = { path, type, name, xml ->
    return new MapEntry(name, JenkinsClient.xmlSerialize(xml))
  } as JenkinsXmlAction

  static JenkinsXmlAction dumpJenkinsXml(File dir) {
    return { String path, ItemType type, String name, xml ->
      (new File(dir, "${path}/${type}s")).mkdirs()
      def dest = new File(dir, "${path}/${type.toString()}s/${name}.xml")
      new XmlNodePrinter(new PrintWriter(dest)).print(xml)
      return new MapEntry(name, dest)
    } as JenkinsXmlAction
  }

  //TODO: handle logging properly
  static JenkinsXmlAction postJenkinsXml(JenkinsClient baseClient) {
    return { String path, ItemType type, String name, Node xml ->
      def client = baseClient.withPath(folderUriPath(path))
      def idString = "${type} '${name}' in ${'/' + path}"
      Node existing = client.getItemXml(type, name)
      UpdateResult result = UpdateResult.unknown

      if (existing == null) {
        println(AnsiRenderer.render("@|green Creating new ${idString} |@"))
        client.createItemXml(type, name, xml)
        result = UpdateResult.created
      } else {
        //Preserve disabled status, as it's an ephemeral property of the job itself
        //NOTE: This doesn't exist on multibranch pipelines!
        def disabledNode = existing.disabled[0]
        String disabledIndicator = ""
        if(disabledNode != null) {
          boolean disabled = existing.disabled[0].value()[0].toBoolean()
          disabledIndicator = disabled ? AnsiRenderer.render("@|red (disabled)|@") : ''
          xml.disabled[0].setValue(existing.disabled[0].value()[0])
        }

        //Don't update unless there are real differences
        Diff diff = XMLUnit.compareXML(JenkinsClient.xmlSerialize(xml), JenkinsClient.xmlSerialize(existing))

        //Pretty print differences in info mode
        if(System.getProperty('printJenkinsDiffs').toBoolean()) {
          diff.overrideDifferenceListener([
                  differenceFound  : { Difference differ ->
                    if (!differ.isRecoverable()) {
                      println(AnsiRenderer.render("@|yellow ${differ.toString()}|@"))
                    }
                    return Diff.RETURN_ACCEPT_DIFFERENCE
                  },
                  skippedComparison: { control, test -> }
          ] as DifferenceListener)
        }

        if (diff.similar()) {
          println("Not updating unchanged ${idString} ${disabledIndicator}")
          result = UpdateResult.unchanged
        } else {
          println(AnsiRenderer.render("@|blue Updating ${idString}|@ ${disabledIndicator}"))
          client.updateItemXml(type, name, xml)
          result = UpdateResult.changed
        }
      }

      return new MapEntry(name, result)
    } as JenkinsXmlAction
  }
}
