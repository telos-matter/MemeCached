# MemeCached &nbsp; ![DEVELOPMENT STATUS: version 0.5](https://badgen.net/badge/DEVELOPMENT%20STATUS/version%200.5/green)
## A replica of [Memcached](https://memcached.org/) in Java but worse.

My only interaction with Memcached is while reading this [article](https://quuxplusone.github.io/blog/2022/01/06/memcached-interview/). So not much experience with it.
I just found its concept interesting and cool, and so I made it for fun.

For those who don't know what Memcached is, ~Memcached is~.. rather, **MemeCached** is simply a HashMap that stores key-value mappings but only for a set amount of time. After that time has passed those values are forgotten about and simply deleted from the map. There is possibility to define a callback that will get called when a value is forgotten about.

## Examples:
Check out the [tests](src/test/java/MemeCachedTest.java#L11) for examples on how to use it.


## Installation:
If you actually want to use this, you can install it:
```console
$ mvn clean install
```

And add the dependency to your project:
```xml
<dependency>
    <groupId>telos-matter</groupId>
    <artifactId>memeCached</artifactId>
    <version>0.5</version>
</dependency>
```
