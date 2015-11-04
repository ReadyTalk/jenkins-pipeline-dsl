package com.readytalk.jenkins.model.visitors

import com.readytalk.jenkins.model.*
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.AnsiRenderWriter
import org.fusesource.jansi.AnsiRenderer

//Print the model dsl (debugging)
//TODO: print the actual tree with full component value context for documentation
//Kind of hard actually since the defaults are filled in implicitly
class ModelPrettyPrinter extends SymmetricVisitor {
  int indentLevel = 0

  def exit(ModelElement _) {
    indentLevel--
  }

  def enter(ItemModelElement element) {
    printWithIndent("Item: ${element.name}@${element.type.name}")
    indentLevel++
  }

  def enter(ComponentModelElement element) {
    printWithIndent("Component: ${element.type.name}")
    indentLevel++
    printWithIndent(element.fields)
  }

  def enter(GroupModelElement element) {
    printWithIndent("Group:")
    indentLevel++
  }

  def printWithIndent(obj) {
    println "${'  '.multiply(indentLevel)}${obj.toString()}"
  }

  //TODO: Disable color codes if terminal doesn't support it - built-in jansi detector doesn't work
  static void printItemList(itemList) {
    itemList.each { ItemSource source ->
      println AnsiRenderer.render("@|blue Item|@ @|cyan '${source.itemName}'|@:")
      source.components.each { component ->
        println AnsiRenderer.render("    @|white ${component.name}|@:")
        component.fields.each { k, v ->
          def value = source.lookupValue(component.name, k)
          String color = ''

          def defaultValue = null
          try {
            defaultValue = source.lookupValue(component.getName(), k, source.defaults)
          } catch(RuntimeException e) {
            if(!e.message.contains('no default value')) {
              throw(e)
            }
          }

          if(value == null) {
            color = 'bold,red'
          } else if(value == component.fields[k]) {
            color = 'faint,white'
          } else if(value == defaultValue) {
            color = 'faint,green'
          } else if(source.user.context.containsKey(k)) {
            color = 'bold,green'
          } else {
            color = 'green'
          }
          String parameterString = k
          String valueString
          if(value instanceof String && value.contains("\n")) {
            valueString = value.split('\n').join("\n${' '.multiply(10 + k.size())}")
          } else if(value instanceof Closure) {
            valueString = "${value.getClass()}"
          } else {
            valueString = value
          }

          if(value instanceof Map || value instanceof TemplateMap) {
            println AnsiRenderer.render("        ${parameterString}:")
            value.each { key, val ->
              println AnsiRenderer.render("            @|${color} ${key}: ${val}|@")
            }
          } else {
            println AnsiRenderer.render("        ${parameterString}: @|${color} ${valueString}|@")
          }
        }
      }
      println ''
    }
  }
}
