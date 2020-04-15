# pricewatch

Reaction Pricewatch watches prices for change notifications.


### REPL Interaction

## Developing

### Setup

- Run `./bin/setup`
- Edit `.env` and customize if necessary

### The REPL Environment

To begin developing, start with a REPL.

```sh
docker-compose run --rm --service-ports web bin/dev
```

Then load the development namespace.

```clojure
user=> (dev)
:loaded
```

Run `go` to prep and initiate the system.

```clojure
dev=> (go)
:initiated
```

### Reloading Code in the REPL

When you make changes to your source files, use `reset` to reload any
modified files and reset the server. The code will be reloaded and the system
will be restarted.

```clojure
dev=> (reset)
:reloading (...)
:resumed
```

### Safely Stopping the REPL

It's best to explicitly stop the system before stopping the REPL else, it can
hang.

```clojure
dev=> (halt)
dev=> :repl/quit
```

If you quit and the process does hang then you can kill it with Docker in
another shell.

```sh
docker-compose kill web
```

### Testing

Testing is fastest through the REPL, as you avoid environment startup time.

```clojure
dev=> (test :unit)
...
```

But you can also run tests through a standalone command:

```sh
docker-compose run --rm web bin/test --watch
```

## License

© 2020 Reaction Commerce

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
