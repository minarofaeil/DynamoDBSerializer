package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class NestedObjectArrayTest {

    private Serializer<HasNestedObject> serializer;

    @BeforeEach
    public void setUp() {
        serializer = ca.fineapps.util.ddb.serializer.NestedObjectArrayTest_HasNestedObjectSerializer.create();
    }

    @Test
    public void testSerialize() {
        NestedObject[] nestedArray = new NestedObject[]{
                new NestedObject("nested-1", 100),
                new NestedObject("nested-2", 200)
        };

        HasNestedObject original = new HasNestedObject(nestedArray);

        Map<String, AttributeValue> map = serializer.serialize(original);

        AttributeValue nestedSerialized = map.get("nestedObjects");
        List<AttributeValue> nestedList = nestedSerialized.l();

        assertThat(nestedList.size(), is(equalTo(2)));

        Map<String, AttributeValue> first = nestedList.get(0).m();
        Map<String, AttributeValue> second = nestedList.get(1).m();

        assertThat(first.get("stringValue").s(), is(equalTo("nested-1")));
        assertThat(first.get("intValue").n(), is(equalTo("100")));

        assertThat(second.get("stringValue").s(), is(equalTo("nested-2")));
        assertThat(second.get("intValue").n(), is(equalTo("200")));
    }

    @Test
    public void testDeserialize() {
        Map<String, AttributeValue> nestedMap1 = new HashMap<>();
        nestedMap1.put("stringValue", AttributeValue.builder().s("nested-1").build());
        nestedMap1.put("intValue", AttributeValue.builder().n("100").build());

        Map<String, AttributeValue> nestedMap2 = new HashMap<>();
        nestedMap2.put("stringValue", AttributeValue.builder().s("nested-2").build());
        nestedMap2.put("intValue", AttributeValue.builder().n("200").build());

        List<AttributeValue> nestedList = new ArrayList<>();
        nestedList.add(AttributeValue.builder().m(nestedMap1).build());
        nestedList.add(AttributeValue.builder().m(nestedMap2).build());

        Map<String, AttributeValue> map = new HashMap<>();
        map.put("nestedObjects", AttributeValue.builder().l(nestedList).build());

        HasNestedObject original = serializer.deserialize(map);

        NestedObject[] nestedArray = original.nestedObjects();
        assertThat(nestedArray, is(not(nullValue())));
        assertThat(nestedArray.length, is(equalTo(2)));

        assertThat(nestedArray[0].stringValue(), is(equalTo("nested-1")));
        assertThat(nestedArray[0].intValue(), is(equalTo(100)));

        assertThat(nestedArray[1].stringValue(), is(equalTo("nested-2")));
        assertThat(nestedArray[1].intValue(), is(equalTo(200)));
    }

    @Test
    public void testDeserializeWithoutNestedObject() {
        Map<String, AttributeValue> map = new HashMap<>();

        HasNestedObject original = serializer.deserialize(map);

        assertThat(original.nestedObjects(), is(nullValue()));
    }

    @Serialize
    record HasNestedObject(
            NestedObject[] nestedObjects
    ) {
    }

    record NestedObject(
            String stringValue,
            int intValue
    ) {
    }
}
