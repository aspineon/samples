package org.immutables.samples.json;

import org.immutables.samples.json.pojo.OptionalTypeAdapterFactory;
import com.google.gson.GsonBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.marshal.Marshaling;
import org.immutables.samples.json.autojackson.AutoDocument;
import org.immutables.samples.json.immutables.ImDocument;
import org.immutables.samples.json.immutables.ImDocumentStreamer;
import org.immutables.samples.json.pojo.PojoDocument;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class JsonBenchmarks {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private String json;
  private Gson gson;

  @Setup
  public void setup() throws IOException {
    json = Resources.toString(JsonBenchmarks.class.getResource("sample.json"), StandardCharsets.UTF_8);
    objectMapper.registerModule(new GuavaModule());
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(new OptionalTypeAdapterFactory())
        .create();
  }

  @Benchmark
  public String autojackson() throws IOException {
    AutoDocument document = objectMapper.readValue(json, AutoDocument.class);
    return objectMapper.writeValueAsString(document);
  }

  @Benchmark
  public String pojojackson() throws IOException {
    PojoDocument document = objectMapper.readValue(json, PojoDocument.class);
    return objectMapper.writeValueAsString(document);
  }

  @Benchmark
  public String pojoGson() {
    PojoDocument document = gson.fromJson(json, PojoDocument.class);
    return gson.toJson(document);
  }

  @SuppressWarnings("resource")
  @Benchmark
  public String immutablesGson() throws IOException {
    JsonReader reader = new JsonReader(new StringReader(json));

    ImDocumentStreamer streamer = ImDocumentStreamer.instance();
    ImDocument document = streamer.unmarshalInstance(reader);

    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    streamer.marshalInstance(writer, document);
    writer.close();
    return stringWriter.toString();
  }

  // Marshaling.toJson/fromJson is not used due to pretty printing enabled by default
  @SuppressWarnings("resource")
  @Benchmark
  public String immutables() throws IOException {
    Marshaler<ImDocument> marshaler = Marshaling.marshalerFor(ImDocument.class);

    JsonParser parser = objectMapper.getFactory().createParser(json);
    ImDocument document = marshaler.unmarshalInstance(parser);

    StringWriter stringWriter = new StringWriter();
    JsonGenerator generator = objectMapper.getFactory().createGenerator(stringWriter);
    marshaler.marshalInstance(generator, document);
    generator.close();

    return stringWriter.toString();
  }
}
