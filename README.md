# seqd

Somewhere in between a [snowflake](https://github.com/twitter/snowflake) and a [noeqd](https://github.com/bmizerany/noeqd).

# Motivation

Most of the inspiration for this project comes from twitter's Snowflake generator with ideals like, simple setup, configuration, and wireprotocols, borrowed from noeqd. Goals (differences) this project has are

* modularity. Seqd can be used as a server _or_ an in-process id generator without network library dependencies
* simplicity. Snowflake is written in entangles many twitter specific concerns throughout its source. seqd makes as few assumptions about its deployment as possible.
* introspection. One of the key features of both snowflake and noeqd is the encoding of meta information within generator ids but neither exposes a straight forward way to access this information. seqd does.


Doug Tangren (softprops) 2014
