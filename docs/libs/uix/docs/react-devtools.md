<!-- ai: {:tags [:libs] :kind :reference} -->

# React DevTools

[React DevTools](https://chrome.google.com/webstore/detail/react-developer-tools/fmkadmapgofadopljbjfkapdkoienihi?hl=en) is a browser extension that can be used to inspect live UI tree, component props and profile performance in DevTools.

As an alternative, there's [cljs-react-devtools](https://github.com/roman01la/cljs-react-devtools/), which is similar to React DevTools, but targets ClojureScript wrappers specifically to make sure that Clojure data structures are inspectable, as well as Reagent reactions and re-frame subscriptions.

## UIx specifics

- Jump to component definition button [2] works as expected, unlike in Reagent
- Component's props preview [3] displays JS implementation of props map instead of using Custom Formatters. The workaround for this is to use debug button [1] that will print component's data to browser console where a custom formatter ([here's how to install and enable custom formatters in Chrome](https://github.com/binaryage/cljs-devtools/blob/master/docs/installation.md#enable-custom-formatters-in-chrome)) will print props map correctly [4]

![](/docs/rdt_1.jpg)
