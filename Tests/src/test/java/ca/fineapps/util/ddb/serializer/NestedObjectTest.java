package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

public class NestedObjectTest {

    private Serializer<HasNestedObject> serializer;

    @BeforeEach
    public void setUp() {
        serializer = ca.fineapps.util.ddb.serializer.NestedObjectTest_HasNestedObjectSerializer.create();
    }

    @Test
    public void testSerialize() {
        NestedObject nested = new NestedObject(
                "nested-string",
                200
        );

        HasNestedObject original = new HasNestedObject(
                "root-string",
                100,
                nested
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        // Top-level fields
        assertThat(map.get("stringValue").s(), is(equalTo("root-string")));
        assertThat(map.get("intValue").n(), is(equalTo("100")));

        // Nested object
        AttributeValue nestedSerialized = map.get("nestedObject");
        Map<String, AttributeValue> nestedMap = nestedSerialized.m();

        assertThat(nestedMap.get("stringValue").s(), is(equalTo("nested-string")));
        assertThat(nestedMap.get("intValue").n(), is(equalTo("200")));
    }

    @Test
    public void testDeserialize() {
        Map<String, AttributeValue> nestedMap = new HashMap<>();
        nestedMap.put("stringValue", AttributeValue.builder().s("nested-string").build());
        nestedMap.put("intValue", AttributeValue.builder().n("200").build());

        Map<String, AttributeValue> map = new HashMap<>();
        map.put("stringValue", AttributeValue.builder().s("root-string").build());
        map.put("intValue", AttributeValue.builder().n("100").build());
        map.put("nestedObject", AttributeValue.builder().m(nestedMap).build());

        HasNestedObject original = serializer.deserialize(map);

        assertThat(original.stringValue(), is(equalTo("root-string")));
        assertThat(original.intValue(), is(equalTo(100)));

        NestedObject nested = original.nestedObject();
        assertThat(nested.stringValue(), is(equalTo("nested-string")));
        assertThat(nested.intValue(), is(equalTo(200)));
    }

    @Test
    public void testDeserializeWithoutNestedObject() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("stringValue", AttributeValue.builder().s("root-string").build());
        map.put("intValue", AttributeValue.builder().n("100").build());

        HasNestedObject original = serializer.deserialize(map);

        assertThat(original.stringValue(), is(equalTo("root-string")));
        assertThat(original.intValue(), is(equalTo(100)));
        assertThat(original.nestedObject(), is(nullValue()));
    }

    @Serialize
    record HasNestedObject(
            String stringValue,
            int intValue,
            NestedObject nestedObject
    ) {
    }

    record NestedObject(
            String stringValue,
            int intValue
    ) {
    }
}
