import static java.nio.charset.StandardCharsets.UTF_8;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.UTF8StringMarshaller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLoad {

    private static final List<String> wordList = Arrays.asList("foo", "bar", "test");
    private static final Random rand = new Random();
    protected int phraseSize;
    protected static String cacheName;
    protected Double version;

    protected void execute(String[] args) {
        String USAGE = "\nUsage: load.sh --entries num [--write-batch num] [--phrase-size num] [--hotrodversion num]\n";
        Runnable usage = () -> System.out.println(USAGE);

        if (args.length == 0 || args.length % 2 != 0) {
            usage.run();
            return;
        }

        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i = i + 2) {
            String option = args[i];
            if (!option.startsWith("--")) {
                usage.run();
                return;
            }
            options.put(option.substring(2), args[i + 1]);
        }
        int entries;

        String entriesValue = options.get("entries");
        String writeBatchValue = options.get("write-batch");
        String phraseSizeValue = options.get("phrase-size");
        String protocolValue = options.get("hotrodversion");
        cacheName = options.get("cache-name");
        version = Double.parseDouble(options.get("version"));

        phraseSize = phraseSizeValue != null ? Integer.parseInt(phraseSizeValue) : 10;
        final int write_batch = writeBatchValue != null ? Integer.parseInt(writeBatchValue) : 10000;
        if (entriesValue == null) {
            System.out.println("option 'entries' is required");
            usage.run();
            return;
        } else {
            entries = Integer.parseInt(entriesValue);
        }

        ConfigurationBuilder clientBuilder = getConfigurationBuilder();
        if(protocolValue != null) clientBuilder.protocolVersion(protocolValue);
        RemoteCacheManager rcm = new RemoteCacheManager(clientBuilder.build());

        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        AtomicInteger counter = new AtomicInteger();
        CompletableFuture<?>[] futures = new CompletableFuture[nThreads];
        final int totalEntries = entries;
        RemoteCache<String, Object> cache = getCache(rcm, cacheName);
        cache.clear();
        for (int i = 0; i < nThreads; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                Map<String, Object> group = new HashMap<>();
                for(int j = counter.incrementAndGet(); j <= totalEntries; j = counter.incrementAndGet()) {
                    group.put(String.valueOf(j), generateCacheValues());
                    if (group.size() == write_batch) {
                        cache.putAll(group);
                        group = new HashMap<>();
                    }
                }
                cache.putAll(group);
                return null;
            }, executorService);
        }
        CompletableFuture.allOf(futures).join();
        executorService.shutdownNow();
    }

    protected ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder clientBuilder =  new ConfigurationBuilder();
        clientBuilder.addServer().host("localhost").port(11222);

        if(version >= 8.1) {
            clientBuilder.security().authentication()
                    .username("user")
                    .password("passwd-123")
                    .realm("default")
                    .saslMechanism("DIGEST-MD5")
                    .serverName("infinispan");
        }

        clientBuilder.marshaller(new UTF8StringMarshaller());
        return clientBuilder;
    }

    abstract RemoteCache getCache(RemoteCacheManager rcm, String cacheName);

    public abstract Object generateCacheValues();

    public static String randomWord() {
        return wordList.get(rand.nextInt(wordList.size()));
    }

}