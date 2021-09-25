import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
        includeClasses = {
                Book.class,
                Author.class
        },
        schemaFileName = "library.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "book_sample")
public interface LibraryInitializer extends SerializationContextInitializer {
}