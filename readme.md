# threes

Playing around with three.js. Super out of my depth on this one.

## Overview

Not really anything meant to be anything. The rest of this doc is
boiler plate from the `mies` template. Might be useful.

## Setup

First-time Clojurescript developers, add the following to your bash .profile:

    export LEIN_FAST_TRAMPOLINE=y
    alias cljsbuild="lein trampoline cljsbuild $@"

To avoid compiling ClojureScript for each build, AOT Clojurescript
locally in your project with the following:

    ./scripts/compile_cljsc

Subsequent dev builds can use:

    lein cljsbuild auto dev

To start a Node REPL (requires rlwrap):

    ./scripts/repl

To get source map support in the Node REPL:

    lein npm install

Clean project specific out:

    lein clean

Optimized builds:

    lein cljsbuild once release

For more info on Cljs compilation, read
[Waitin'](http://swannodette.github.io/2014/12/22/waitin/).

## License

Copyright (c) 2014 Keith Irwin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
