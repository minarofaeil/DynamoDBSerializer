package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

public class EnumTest {

    private Serializer<EnumContainer> serializer;

    @BeforeEach
    public void setUp() {
        serializer = EnumTest_EnumContainerSerializer.create();
    }

    @Test
    public void testSerialize() {
        EnumContainer enumContainer = new EnumContainer(
                EnumContainer.AnEnum.VALUE1,
                new EnumContainer.AnEnum[] { EnumContainer.AnEnum.VALUE1, EnumContainer.AnEnum.VALUE2 },
                Arrays.asList(EnumContainer.AnEnum.VALUE1, EnumContainer.AnEnum.VALUE2)
        );

        Map<String, AttributeValue> serialized = serializer.serialize(enumContainer);
        assertThat(serialized, is(aMapWithSize(3)));

        assertThat(serialized, hasEntry("value", AttributeValue.fromS("VALUE1")));
        assertThat(serialized, hasEntry("enumArray", AttributeValue.fromSs(Arrays.asList("VALUE1", "VALUE2"))));
        assertThat(serialized, hasEntry("enumList", AttributeValue.fromSs(Arrays.asList("VALUE1", "VALUE2"))));
    }

    @Test
    public void testDeserialize() {
        Map<String, AttributeValue> map = Map.of(
                "value", AttributeValue.fromS("VALUE2"),
                "enumArray", AttributeValue.fromSs(Arrays.asList("VALUE2", "VALUE1")),
                "enumList", AttributeValue.fromSs(Arrays.asList("VALUE2", "VALUE1"))
        );

        EnumContainer enumContainer = serializer.deserialize(map);
        assertThat(enumContainer.value(), is(EnumContainer.AnEnum.VALUE2));
        assertThat(enumContainer.enumArray(), is(equalTo(new EnumContainer.AnEnum[] {
                EnumContainer.AnEnum.VALUE2,
                EnumContainer.AnEnum.VALUE1,
        })));
        assertThat(enumContainer.enumList(), is(equalTo(Arrays.asList(
                EnumContainer.AnEnum.VALUE2,
                EnumContainer.AnEnum.VALUE1
        ))));
    }

    @Test
    public void testSerializeWithNull() {
        EnumContainer enumContainer = new EnumContainer(null, null, null);
        Map<String, AttributeValue> serialized = serializer.serialize(enumContainer);
        assertThat(serialized, is(anEmptyMap()));
    }

    @Test
    public void testDeserializeWithNull() {
        Map<String, AttributeValue> map = Collections.emptyMap();
        EnumContainer enumContainer = serializer.deserialize(map);

        assertThat(enumContainer.value(), is(nullValue()));
        assertThat(enumContainer.enumArray(), is(nullValue()));
        assertThat(enumContainer.enumList(), is(nullValue()));
    }

    @Serialize
    record EnumContainer(AnEnum value, AnEnum[] enumArray, List<AnEnum> enumList) {
        enum AnEnum {
            VALUE1,
            VALUE2,
        }
    }
}
