#Changelog
----------

* 0.12.0
    - Update upstream job-dsl-core from 1.37 to 1.39
    - Enable multiple dsl blocks per component
    - Add git clean exclusion patterns for git component
        Uses shell to run git clean instead of builtin plugin

* 0.11.1
    - Fix duplicated description and enable templated descriptions

* 0.11.0
    - Add extEmail component for ExtendedEmailPlugin
    - Add blockOn component for locks and build blocking config
    - Add overrides map for pullRequest component
        Note - overrides block is experimental
        Ideally, it should be an actual chunk of parsed dsl
    - Aggregate description field by default
    - Minor cleanup and refactoring

* 0.10.3
    - Fix polling for pull request support using mergeTo
    - Use direct refspec for Stash pull requests instead of remapping
    - Include target branch in refspec to ensure merge is up-to-date

* 0.10.1
    - Refactored ContextMap to use Optional instead of null checking
    - Now throws explicit error if field with required value left unset
    - Context values are now immutable within the same scope by default
    - Now locally merges PRs by default before building
    - Fixed matrix job type to actually set job type as matrixJob
    - Removed JobTypeOverride as it didn't work as intended
    - Requires Java 8 due to Optional
      This may get reverted if it causes too many compatibility issues,
      and use an alternate implementation of Optional instead

* 0.8.3
    - Added context to errors such as yaml or groovy file paths
    - Added "blankJob" type for jobs that shouldn't pre-apply git
    - Updated to use upstream job-dsl-core version 1.37

* 0.8.2
    - Added matrix job support (very minimal - drops down to netflix dsl for
      axes definition for now)
    - Better support for ComponentAdapters. This enables extracting out
      common logic from Components that usually ended up in the postProcess
      block. Like the postProcess block, they should be avoided if possible,
      and are provided as an abstraction for syntactic sugar and special cases.
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
