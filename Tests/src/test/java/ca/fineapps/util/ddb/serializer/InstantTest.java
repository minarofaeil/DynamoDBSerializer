package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class InstantTest {

    private Serializer<TimeContainer> serializer;

    @BeforeEach
    public void setUp() {
        serializer = ca.fineapps.util.ddb.serializer.InstantTest_TimeContainerSerializer.create();
    }

    @Test
    public void testSerialize() {
        Instant time = Instant.ofEpochMilli(1700000000000L);
        TimeContainer original = new TimeContainer(time);

        Map<String, AttributeValue> map = serializer.serialize(original);
        assertThat(map.get("time").n(), is(equalTo("1700000000000")));
    }

    @Test
    public void testDeserialize() {
        long millis = 1700000000000L;

        Map<String, AttributeValue> map = new HashMap<>();
        map.put("time", AttributeValue.builder().n("1700000000000").build());

        TimeContainer deserialized = serializer.deserialize(map);
        assertThat(deserialized.time(), is(equalTo(Instant.ofEpochMilli(millis))));
    }

    @Test
    public void testSerializeDefault() {
        TimeContainer original = new TimeContainer(null);

        Map<String, AttributeValue> map = serializer.serialize(original);
        assertThat(map.containsKey("time"), is(false));
    }

    @Test
    public void testDeserializeDefault() {
        Map<String, AttributeValue> map = new HashMap<>();

        TimeContainer deserialized = serializer.deserialize(map);
        assertThat(deserialized.time(), is(equalTo(null)));
    }

    @Serialize
    record TimeContainer(Instant time) {
    }
}
