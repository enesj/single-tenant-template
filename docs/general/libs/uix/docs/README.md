<!-- ai: {:tags [:libs :uix] :kind :overview} -->

<img src="logo.png" width="125" />

_Idiomatic ClojureScript interface to modern React.js_

> "Oh god, I just started learning reagent. Don’t tell me I’ll have to switch" /r/clojure

[![CircleCI](https://circleci.com/gh/pitch-io/uix.svg?style=svg)](https://circleci.com/gh/pitch-io/uix)
[![Clojars Project](https://img.shields.io/clojars/v/com.pitch/uix.core.svg)](https://clojars.org/com.pitch/uix.core)
[![Clojars Project](https://img.shields.io/clojars/v/com.pitch/uix.dom.svg)](https://clojars.org/com.pitch/uix.dom)

- API compatibility: React v19.0.0
- Discuss at [#uix on Clojurians Slack](https://clojurians.slack.com/archives/CNMR41NKB)
- Try it out in the [playground](https://studio.learn-modern-clojurescript.com/p/default-uix)
- [A slide deck explaining UIx and migration path from Reagent](https://pitch.com/public/821ed924-6fe6-4ce7-9d75-a63f1ee3c61f)
- [Talk about UIx at London Clojurians meetup](https://www.youtube.com/watch?v=4vgrLHsD0-I)
- ["The State of Frontend" by Alexander Davis](https://www.youtube.com/watch?v=fT28NeZtaAg)
- [ClojureStream podcast: E94 UIx with Roman Liutikov](https://soundcloud.com/clojurestream/e94-uix-with-roman-liutikov)

## Installation

```
npm install react@19.0.0 react-dom@19.0.0 --save-dev
```

```clj
{:deps {com.pitch/uix.core {:mvn/version "1.3.1"}
        com.pitch/uix.dom {:mvn/version "1.3.1"}}}
```

### How to start a new project with UIx

- Run `npx create-uix-app@latest my-app` to scaffold a new project
- Clone starter template manually from [pitch-io/uix-starter](https://github.com/pitch-io/uix-starter)
- Use fullstack starter project from Metosin [metosin/example-project](https://github.com/metosin/example-project)

## Usage

```clj
(ns my.app
  (:require [uix.core :refer [defui $]]
            [uix.dom]))

(defui button [{:keys [on-click children]}]
  ($ :button.btn {:on-click on-click}
    children))

(defui app []
  (let [[state set-state!] (uix.core/use-state 0)]
    ($ :<>
      ($ button {:on-click #(set-state! dec)} "-")
      ($ :span state)
      ($ button {:on-click #(set-state! inc)} "+"))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))

(uix.dom/render-root ($ app) root)
```

## Docs

- [What is UIx?](what-is-uix.md)
- [Components](components.md)
- [Elements](elements.md)
- [Hooks](hooks.md)
- [State](state.md)
- [Effects](effects.md)
- [Props validation](props-validation.md)
  - [Compile-time props validation](props-validation.md#compile-time-props-validation)
- [Interop with React](interop-with-react.md)
- [Interop with Reagent](interop-with-reagent.md)
- [Code-splitting and React.lazy](code-splitting.md)
- [Migrating from Reagent](migrating-from-reagent.md)
- [Server-side rendering](server-side-rendering.md)
- [Hot reloading](hot-reloading.md)
- [React DevTools](react-devtools.md)
- [Code linting](code-linting.md)
- [Differences from Reagent](differences-from-reagent.md)
- [Utilities](utilities.md)
- [Examples](/core/dev/uix/examples.cljs)
- [Internals](internals.md)
- Other render targets
  - [React Native](/react-native.md)
  - [React Three Fiber](/react-three-fiber.md)
- [Getting help from ChatGPT](/chat-gpt.md)

## Who's using UIx?

- [Pitch](https://pitch.com/)
- [Pitch iOS app](https://apps.apple.com/us/app/pitch-collaborate-on-decks/id1551335606?platform=iphone)
- [Cognician](https://info.cognician.com/)
- [Multiply](https://multiply.co/)
- [Totcal](https://totcal.com/)
- [ClojureScript Studio](https://studio.learn-modern-clojurescript.com/)
- [Ogres, virtual tabletop](https://ogres.app/)
- [ShipClojure](https://www.shipclojure.com/)
- [Metosin](https://www.metosin.fi/en)

## Contributing

There are several ways how you can contribute to the project:

- Improve documentation: cover missing pieces, add code examples
- Add, improve docstrings
- Propose and implement improvements and new features
- File and fix bugs
- Increase test coverage

## Support

You can support this project via [Github Sponsors](https://github.com/sponsors/roman01la) or [Buy Me a Coffee](https://buymeacoffee.com/romanliutikov).

## Testing

```
scripts/test
```

_Note: to ensure you're using the right Node.js version, you can use [nvm](https://github.com/nvm-sh/nvm) and run `nvm use`
once in the directory. Otherwise the Node.js version you use is in the `.nvmrc` file. See nvm repo for more documentation._

## Thanks to

- [UIx v1](https://github.com/roman01la/uix) for initial set of ideas and learnings
- [Helix](https://github.com/lilactown/helix) for even more ideas
- [Pitch](https://github.com/pitch-io) for sponsoring initial development and dogfooding the work
