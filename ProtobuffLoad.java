///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.infinispan:infinispan-bom:12.1.3.Final@pom
//DEPS org.infinispan:infinispan-client-hotrod
//DEPS org.infinispan.protostream:protostream-processor
//DEPS org.infinispan.protostream:protostream
//DEPS org.jeasy:easy-random-core:5.0.0
//SOURCES Author.java
//SOURCES Book.java
//SOURCES LibraryInitializer.java
//SOURCES AbstractLoad.java
//SOURCES ProtobuffLoad.java

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jeasy.random.EasyRandom;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

public class ProtobuffLoad extends AbstractLoad {

    @Override
    RemoteCache getCache(RemoteCacheManager rcm, String cacheName) {
        RemoteCache<String, Book> cache = rcm.getCache(cacheName);
        return cache;
    }

    @Override
    public Book generateCacheValues() {
        return new EasyRandom().nextObject(Book.class);
    }

    @Override
    public ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder configurationBuilder = super.getConfigurationBuilder();
        configurationBuilder.addContextInitializer(new LibraryInitializerImpl());
        return configurationBuilder;
    }
}