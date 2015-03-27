# lein-azkaban

A Leiningen plugin to interact with [Azkaban](http://azkaban.github.io).

## Usage

Add a key called `:azkaban` to your project.clj containing the following keys:

* `:endpoint` - the location of the azkaban management server
* `:project` - the name of the project in azkaban
* `:username` - the username to authenticate to azkaban as
* `:password` - the password to authenticate to azkaban with

```clojure
{:azkaban {
    :endpoint "http://localhost:8081"
    :project "my-project"
    :username "myuser"
    :password "mypass"
    }}
```
Supported commands are:

* `lein azkaban upload <file>` where `<file>` is a path to your project zip archive.
* `lein azkaban execute <flow> <job_override>` where `<flow>` is the name of a flow in the azkaban project, `<job_override>` are key value pairs that override default job parameters for this execution.

Use this for user-level plugins:

Put `[lein-azkaban "0.1.0"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
lein-azkaban 0.1.0`.

Use this for project-level plugins:

Put `[lein-azkaban "0.1.0"]` into the `:plugins` vector of your project.clj.

## License

Copyright © 2014 Soren Macbeth

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
