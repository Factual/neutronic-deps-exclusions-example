## Neutronic Dep Problems Test

```
sbt "dependencyTree::toFile deps_tree.txt"
sbt "whatDependsOn com.google.protobuf protobuf-java" > what_depends_on_protobuf.txt
sbt "whatDependsOn org.eclipse.jetty jetty-server" > what_depends_on_jetty-server.txt
```

* [Neutronic Batch Pom](https://github.com/Factual/neutronic/blob/e69ca5d3fe513d10fe0e0224b079af303df09b2a/neutronic-batch/pom.xml)
* [shaded-solr-indexing Pom](https://github.com/Factual/shaded-solr/blob/b4a1e12b427f2cc8c4087e65e80f7da72e3a744b/shaded-solr-indexing/pom.xml)
* [shaded-solor-indexing published POM](http://maven.corp.factual.com/nexus/content/groups/public/com/factual/shaded-solr-indexing/7.7.2.0/shaded-solr-indexing-7.7.2.0.pom) - **this is important b/c it's what actually gets to maven after all of the parent/child machinery is resolved**
* [Possible Coursier issue we might also be running into?](https://github.com/coursier/coursier/issues/853)

### Notes

#### SBT appears to see protobuf 3.1.0 as a direct dep of shaded-solr-indexing

This was suprising to me given that it's not listed in that project's POM as a top-level dep (it should be transitive -- e.g. no explicit protobuf dep listed [here](https://github.com/Factual/shaded-solr/blob/b4a1e12b427f2cc8c4087e65e80f7da72e3a744b/shaded-solr-indexing/pom.xml#L16-L25)).

snippet from `what_depends_on_protobuf.txt` (note protobuf is directly under `com.factual:shaded-solr-indexing:7.7.2.5` -- no transitive node in between):

```
[info] com.google.protobuf:protobuf-java:3.1.0
[info]   +-com.factual:shaded-solr-indexing:7.7.2.5
[info]   | +-com.factual:neutronic-batch:2.16.1
```

However, when you look in the actual [**published POM on maven**](http://maven.corp.factual.com/nexus/content/groups/public/com/factual/shaded-solr-indexing/7.7.2.0/shaded-solr-indexing-7.7.2.0.pom), it does appear there:

```
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <parent>
    <artifactId>shaded-solr-parent</artifactId>
    <groupId>com.factual</groupId>
    <version>7.7.2.0</version>
  </parent>
  <modelVersion>4.0.0</modelVersion>
  <artifactId>shaded-solr-indexing</artifactId>
  <name>factual-shaded-solr-indexing</name>
  <build>
    <plugins>
    </plugins>
  </build>
  <dependencies>
    <dependency>
      <groupId>com.google.protobuf</groupId>
      <artifactId>protobuf-java</artifactId>
      <version>3.1.0</version>
      <scope>compile</scope>
    </dependency>
    <TRUNCATED ..../>
  </dependencies>
</project>
```

This seems counter-intuitive to me and I don't have an explanation for why it's happening. It makes me wonder if something about how we're building these JARs with the parent/child module setup in maven isn't behaving the way we'd expect???

#### Protobuf 3.1.0 appears multiple places in the shaded-solr-indexing tree anyway

Even if our attempt to exclude solr-core via neutronic-batch is working, we seem to see Protobuf 3.1.0 in a bunch of _other_ transitive deps anyway, e.g.:

```
[info]   +-org.apache.calcite.avatica:avatica-core:1.10.0
[info]   | +-com.factual:shaded-solr-indexing:7.7.2.5
[info]   | | +-com.factual:neutronic-batch:2.16.1
[info]   | | | +-com.example:deps-test_2.13:0.1.0-SNAPSHOT [S]
```

Now, we might not expect `org.apache.calcite.avatica:avatica-core:1.10.0` to show up as a direct dep of `shaded-solr-indexing` in the first place, but maybe that's another manifestation of whatever is going on in the previous problem.

#### SBT may not be honoring transitive maven exclusions?

I'm struggling to say for sure on this one because it's hard to untangle it from the previous issues, but it seems there may also be problems where SBT does not honor transitive exclusions from POM files.

So, excluding a package in sbt seems to work fine. But if an included POM has exclusions of its own via XML, those seem to not be honored.


For example from `what_depends_on_jetty-server.txt` we see this dep chain:

```
[info]   +-org.eclipse.jetty:jetty-security:9.4.15.v20190215
[info]     +-com.factual:shaded-solr-indexing:7.7.2.5
[info]     | +-com.factual:neutronic-batch:2.16.1
[info]     | | +-com.example:deps-test_2.13:0.1.0-SNAPSHOT [S]
[info]     | |
[info]     | +-com.factual:neutronic-solr:2.16.1
[info]     |   +-com.factual:neutronic-batch:2.16.1
[info]     |     +-com.example:deps-test_2.13:0.1.0-SNAPSHOT [S]
```

I would have thought that this would be blocked by this exclusion in neutronic-batch POM:

```
    <dependency>
      <groupId>com.factual</groupId>
      <artifactId>shaded-solr-indexing</artifactId>
      <exclusions>
        <exclusion>
          <groupId>org.eclipse.jetty</groupId>
          <artifactId>jetty-server</artifactId>
        </exclusion>
        <exclusion>
```

But it may be that this is a misunderstanding of how transitive exclusions are supposed to work -- I'm not 100% sure and would need to read into this more.
