package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class MultipleNestedTypesTest {

    private Serializer<HasMultipleNestedTypes> serializer;

    @BeforeEach
    public void setUp() {
        serializer = ca.fineapps.util.ddb.serializer.MultipleNestedTypesTest_HasMultipleNestedTypesSerializer.create();
    }

    @Test
    public void testSerialize() {
        HasMultipleNestedTypes original = new HasMultipleNestedTypes(
                new FirstNestedType("first"),
                new SecondNestedType("second")
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        Map<String, AttributeValue> firstNestedMap = map.get("firstNestedType").m();
        Map<String, AttributeValue> secondNestedMap = map.get("secondNestedType").m();

        assertThat(firstNestedMap.get("value1").s(), is(equalTo("first")));
        assertThat(secondNestedMap.get("value2").s(), is(equalTo("second")));
    }

    @Test
    public void testDeserialize() {
        Map<String, AttributeValue> map = new HashMap<>();

        Map<String, AttributeValue> firstNestedMap = new HashMap<>();
        firstNestedMap.put("value1", AttributeValue.builder().s("first").build());

        Map<String, AttributeValue> secondNestedMap = new HashMap<>();
        secondNestedMap.put("value2", AttributeValue.builder().s("second").build());

        map.put("firstNestedType", AttributeValue.builder().m(firstNestedMap).build());
        map.put("secondNestedType", AttributeValue.builder().m(secondNestedMap).build());

        HasMultipleNestedTypes obj = serializer.deserialize(map);

        assertThat(obj.firstNestedType().value1(), is(equalTo("first")));
        assertThat(obj.secondNestedType().value2(), is(equalTo("second")));
    }

    @Serialize
    record HasMultipleNestedTypes(
            FirstNestedType firstNestedType,
            SecondNestedType secondNestedType
    ) {
    }

    record FirstNestedType(String value1) {
    }

    record SecondNestedType(String value2) {
    }
}
