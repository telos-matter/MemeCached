# MemeCached &nbsp; ![DEVELOPMENT STATUS: version 0.3](https://badgen.net/badge/DEVELOPMENT%20STATUS/version%200.3/green)
## A replica of [Memcached](https://memcached.org/) in Java but alot worse probably

Reason why I'm saying probably, is because my only interaction with Memcached is while reading this [article](https://quuxplusone.github.io/blog/2022/01/06/memcached-interview/).
I just found its concept interessting and cool, and so I made it for fun.

For those who don't know what Memcached is, ~Memcached is~.. rather, **MemeCached** is simply a HashMap that stores key-value (non-null values only) mappings but only for a set amount of time. After that time has passed those values are forgotten about and simply deleted from the map. Here is a simple [example](UsageExample.java)
