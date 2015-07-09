#Changelog
----------

* 0.6.0-SNAPSHOT
    - Experimental support for templated strings
        This allows for dynamic component defaults
    - Watches tags by default, git.watch field removed
        Branches are what we actually care about building, and pull-request refspecs
        can be handled by the pull-request component
    - Refactored git component to support github by default
    - git component now has a provider field that accepts either stash or github
    - Full reflection-based import
    - Simplified script running
    - Factor out readytalk-specific stuff

* 0.4.0-SNAPSHOT
    - Added support for YAML-based syntax
    - Marked sections of codebase that need to be addressed before open sourcing
