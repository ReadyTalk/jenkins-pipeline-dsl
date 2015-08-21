#Changelog
----------

* 0.8.x
    - Added matrix job support (very minimal - drops down to netflix dsl for
      axes definition for now)
    - Full fledged support for ComponentAdapters. This enables extracting out
      common logic from Components that usually ended up in the postProcess
      block. For example, I have a JobTypeOverride adapter that can detect
      conflicts between components trying to set incompatible job types
    - Pull request builds now build concurrently by default unless explicitly
      disabled in the parent scope
    - Allow setting includedRegions on the git component

* 0.7.6
    - Force workspace polling by default for git
        Remote polling using ls-remote turns out to be very fragile, and newer
        versions of the Jenkins git plugin can auto-provision workspaces for
        polling

* 0.7.5
    - Initial release version
    - Various cleanup and bug fixes
    - Pull requests now default to disabling active notifications
        Still notifies github/stash, but not email/hipchat
    - Plugin now allows setting global default values and templates
        Useful for organization-wide defaults and settings

* 0.6.0-SNAPSHOT
    - Experimental support for templated strings
        This allows for dynamic component defaults
    - Watches tags by default, git.watch field removed
        Branches are what we actually care about building, and pull-request
        refspecs can be handled by the pull-request component
    - Refactored git component to support github by default
    - git component now has a provider field that accepts either stash or github
    - Full reflection-based import
    - Simplified script running
    - Factor out readytalk-specific stuff

* 0.4.0-SNAPSHOT
    - Added support for YAML-based syntax
    - Marked sections of codebase that need to be addressed before open sourcing
