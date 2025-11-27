package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class PrimitiveTest {
    private Serializer<PrimitivesType> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.PrimitiveTest_PrimitivesTypeSerializer();
    }

    @Test
    public void testSerialize() {
        PrimitivesType original = new PrimitivesType(
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

        assertThat(map.get("anInt").n(), is(equalTo("42")));
        assertThat(map.get("aShort").n(), is(equalTo("7")));
        assertThat(map.get("aLong").n(), is(equalTo("123456789")));
        assertThat(map.get("aFloat").n(), is(equalTo(Float.toString(1.23f))));
        assertThat(map.get("aDouble").n(), is(equalTo(Double.toString(4.56))));
        assertThat(map.get("aChar").s(), is(equalTo("x")));
        assertThat(map.get("aByte").n(), is(equalTo("3")));
        assertThat(map.get("aBoolean").bool(), is(true));
    }

    @Test
    public void testDeserialize() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("anInt", AttributeValue.builder().n("42").build());
        map.put("aShort", AttributeValue.builder().n("7").build());
        map.put("aLong", AttributeValue.builder().n("123456789").build());
        map.put("aFloat", AttributeValue.builder().n(Float.toString(1.23f)).build());
        map.put("aDouble", AttributeValue.builder().n(Double.toString(4.56)).build());
        map.put("aChar", AttributeValue.builder().s("x").build());
        map.put("aByte", AttributeValue.builder().n("3").build());
        map.put("aBoolean", AttributeValue.builder().bool(true).build());

        PrimitivesType obj = serializer.deserialize(map);

        assertThat(obj.anInt(), is(equalTo(42)));
        assertThat(obj.aShort(), is(equalTo((short) 7)));
        assertThat(obj.aLong(), is(equalTo(123456789L)));
        assertThat(obj.aFloat(), is(equalTo(1.23f)));
        assertThat(obj.aDouble(), is(equalTo(4.56)));
        assertThat(obj.aChar(), is(equalTo('x')));
        assertThat(obj.aByte(), is(equalTo((byte) 3)));
        assertThat(obj.aBoolean(), is(true));
    }

    @Test
    public void testSerializePrimitiveDefaults() {
        PrimitivesType original = new PrimitivesType(
                0,
                (short) 0,
                0L,
                0.0f,
                0.0,
                '\u0000',
                (byte) 0,
                false
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        assertThat(map.get("anInt").n(), is(equalTo("0")));
        assertThat(map.get("aShort").n(), is(equalTo("0")));
        assertThat(map.get("aLong").n(), is(equalTo("0")));
        assertThat(map.get("aFloat").n(), is(equalTo("0.0")));
        assertThat(map.get("aDouble").n(), is(equalTo("0.0")));
        assertThat(map.get("aChar").s(), is(equalTo("\u0000")));
        assertThat(map.get("aByte").n(), is(equalTo("0")));
        assertThat(map.get("aBoolean").bool(), is(equalTo(false)));
    }

    @Test
    public void testDeserializePrimitiveDefaults() {
        Map<String, AttributeValue> map = new HashMap<>();

        PrimitivesType obj = serializer.deserialize(map);

        assertThat(obj.anInt(), is(equalTo(0)));
        assertThat(obj.aShort(), is(equalTo((short) 0)));
        assertThat(obj.aLong(), is(equalTo(0L)));
        assertThat(obj.aFloat(), is(equalTo(0.0f)));
        assertThat(obj.aDouble(), is(equalTo(0.0)));
        assertThat(obj.aChar(), is(equalTo('\u0000')));
        assertThat(obj.aByte(), is(equalTo((byte) 0)));
        assertThat(obj.aBoolean(), is(equalTo(false)));
    }

    @Serialize
    record PrimitivesType(
            int anInt,
            short aShort,
            long aLong,
            float aFloat,
            double aDouble,
            char aChar,
            byte aByte,
            boolean aBoolean
    ) {
    }
}
