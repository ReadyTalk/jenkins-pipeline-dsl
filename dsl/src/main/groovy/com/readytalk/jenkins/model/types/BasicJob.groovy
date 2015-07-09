package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractItemType
import com.readytalk.jenkins.model.Fixed

import java.util.regex.Matcher

/**
 * Base job that virtually all freestyle-jobs should extend from
 */

@Fixed
class BasicJob extends AbstractItemType {
  final String name = 'basicJob'

  List<String> components = ['base', 'common', 'ownership', 'parameterized', 'git']

  Closure defaults = { }
}