package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class WrapperTest {
    private Serializer<WrapperTypes> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.WrapperTest_WrapperTypesSerializer();
    }

    @Test
    public void testSerializeWrapper() {
        WrapperTypes original = new WrapperTypes(
                42,
                (short) 7,
                123456789L,
                1.23f,
                4.56,
                'x',
                (byte) 3,
                true
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        assertThat(map.get("anInteger").n(), is(equalTo("42")));
        assertThat(map.get("aShort").n(), is(equalTo("7")));
        assertThat(map.get("aLong").n(), is(equalTo("123456789")));
        assertThat(map.get("aFloat").n(), is(equalTo(Float.toString(1.23f))));
        assertThat(map.get("aDouble").n(), is(equalTo(Double.toString(4.56))));
        assertThat(map.get("aCharacter").s(), is(equalTo("x")));
        assertThat(map.get("aByte").n(), is(equalTo("3")));
        assertThat(map.get("aBoolean").bool(), is(equalTo(true)));
    }

    @Test
    public void testDeserializeWrapper() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("anInteger", AttributeValue.builder().n("42").build());
        map.put("aShort", AttributeValue.builder().n("7").build());
        map.put("aLong", AttributeValue.builder().n("123456789").build());
        map.put("aFloat", AttributeValue.builder().n(Float.toString(1.23f)).build());
        map.put("aDouble", AttributeValue.builder().n(Double.toString(4.56)).build());
        map.put("aCharacter", AttributeValue.builder().s("x").build());
        map.put("aByte", AttributeValue.builder().n("3").build());
        map.put("aBoolean", AttributeValue.builder().bool(true).build());

        WrapperTypes obj = serializer.deserialize(map);

        assertThat(obj.anInteger(), is(equalTo(42)));
        assertThat(obj.aShort(), is(equalTo((short) 7)));
        assertThat(obj.aLong(), is(equalTo(123456789L)));
        assertThat(obj.aFloat(), is(equalTo(1.23f)));
        assertThat(obj.aDouble(), is(equalTo(4.56)));
        assertThat(obj.aCharacter(), is(equalTo('x')));
        assertThat(obj.aByte(), is(equalTo((byte) 3)));
        assertThat(obj.aBoolean(), is(equalTo(true)));
    }

    @Test
    public void testSerializeWrapperDefaults() {
        WrapperTypes original = new WrapperTypes(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        assertThat(map.containsKey("anInteger"), is(false));
        assertThat(map.containsKey("aShort"), is(false));
        assertThat(map.containsKey("aLong"), is(false));
        assertThat(map.containsKey("aFloat"), is(false));
        assertThat(map.containsKey("aDouble"), is(false));
        assertThat(map.containsKey("aCharacter"), is(false));
        assertThat(map.containsKey("aByte"), is(false));
        assertThat(map.containsKey("aBoolean"), is(false));
    }

    @Test
    public void testDeserializeWrapperDefaults() {
        Map<String, AttributeValue> map = new HashMap<>();

        WrapperTypes obj = serializer.deserialize(map);

        assertThat(obj.anInteger(), is(equalTo(null)));
        assertThat(obj.aShort(), is(equalTo(null)));
        assertThat(obj.aLong(), is(equalTo(null)));
        assertThat(obj.aFloat(), is(equalTo(null)));
        assertThat(obj.aDouble(), is(equalTo(null)));
        assertThat(obj.aCharacter(), is(equalTo(null)));
        assertThat(obj.aByte(), is(equalTo(null)));
        assertThat(obj.aBoolean(), is(equalTo(null)));
    }


    @Serialize
    record WrapperTypes(
            Integer anInteger,
            Short aShort,
            Long aLong,
            Float aFloat,
            Double aDouble,
            Character aCharacter,
            Byte aByte,
            Boolean aBoolean
    ) {
    }
}
