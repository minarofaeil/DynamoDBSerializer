package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class PrimitiveArraysTest {

    private Serializer<PrimitiveArrayTypes> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.PrimitiveArraysTest_PrimitiveArrayTypesSerializer();
    }

    @Test
    public void testSerializePrimitiveArray() {
        PrimitiveArrayTypes original = new PrimitiveArrayTypes(
                new int[] {42, 43},
                new short[] {7, 8},
                new long[] {123456789L, 987654321L},
                new float[] {1.23f, 4.56f},
                new double[] {7.89, 0.12},
                new byte[] {3, 4},
                new char[] {'x', 'y'},
                new boolean[] {true, false}
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        List<String> intNs = map.get("intArray").ns();
        assertThat(intNs, contains("42", "43"));

        List<String> shortNs = map.get("shortArray").ns();
        assertThat(shortNs, contains("7", "8"));

        List<String> longNs = map.get("longArray").ns();
        assertThat(longNs, contains("123456789", "987654321"));

        List<String> floatNs = map.get("floatArray").ns();
        assertThat(floatNs, contains(Float.toString(1.23f), Float.toString(4.56f)));

        List<String> doubleNs = map.get("doubleArray").ns();
        assertThat(doubleNs, contains(Double.toString(7.89), Double.toString(0.12)));

        byte[] bytes = map.get("byteArray").b().asByteArray();
        assertThat(bytes[0], is(equalTo((byte) 3)));
        assertThat(bytes[1], is(equalTo((byte) 4)));

        String charList = map.get("charArray").s();
        assertThat(charList, is(equalTo("xy")));

        List<AttributeValue> boolList = map.get("booleanArray").l();
        assertThat(boolList.get(0).bool(), is(equalTo(true)));
        assertThat(boolList.get(1).bool(), is(equalTo(false)));
    }

    @Test
    public void testDeserializePrimitiveArray() {
        Map<String, AttributeValue> map = new HashMap<>();

        map.put("intArray", AttributeValue.builder().ns("42", "43").build());
        map.put("shortArray", AttributeValue.builder().ns("7", "8").build());
        map.put("longArray", AttributeValue.builder().ns("123456789", "987654321").build());
        map.put("floatArray", AttributeValue.builder().ns("1.23", "4.56").build());
        map.put("doubleArray", AttributeValue.builder().ns("7.89", "0.12").build());
        map.put("byteArray", AttributeValue.builder().b(SdkBytes.fromByteArray(new byte[] {3, 4})).build());
        map.put("charArray", AttributeValue.builder().s("xy").build());
        map.put("booleanArray", AttributeValue.builder()
                .l(AttributeValue.builder().bool(true).build(), AttributeValue.builder().bool(false).build())
                .build());

        PrimitiveArrayTypes obj = serializer.deserialize(map);

        assertThat(obj.intArray(), is(equalTo(new int[] {42, 43})));
        assertThat(obj.shortArray(), is(equalTo(new short[] {7, 8})));
        assertThat(obj.longArray(), is(equalTo(new long[] {123456789L, 987654321L})));
        assertThat(obj.floatArray(), is(equalTo(new float[] {1.23f, 4.56f})));
        assertThat(obj.doubleArray(), is(equalTo(new double[] {7.89, 0.12})));
        assertThat(obj.byteArray(), is(equalTo(new byte[] {3, 4})));
        assertThat(obj.charArray(), is(equalTo(new char[] {'x', 'y'})));
        assertThat(obj.booleanArray(), is(equalTo(new boolean[] {true, false})));
    }

    @Test
    public void testSerializePrimitiveArrayDefaults() {
        PrimitiveArrayTypes original = new PrimitiveArrayTypes(
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

        assertThat(map.containsKey("intArray"), is(false));
        assertThat(map.containsKey("shortArray"), is(false));
        assertThat(map.containsKey("longArray"), is(false));
        assertThat(map.containsKey("floatArray"), is(false));
        assertThat(map.containsKey("doubleArray"), is(false));
        assertThat(map.containsKey("byteArray"), is(false));
        assertThat(map.containsKey("charArray"), is(false));
        assertThat(map.containsKey("booleanArray"), is(false));
    }

    @Test
    public void testDeserializePrimitiveArrayDefaults() {
        Map<String, AttributeValue> map = new HashMap<>();

        PrimitiveArrayTypes obj = serializer.deserialize(map);

        assertThat(obj.intArray(), is(equalTo(null)));
        assertThat(obj.shortArray(), is(equalTo(null)));
        assertThat(obj.longArray(), is(equalTo(null)));
        assertThat(obj.floatArray(), is(equalTo(null)));
        assertThat(obj.doubleArray(), is(equalTo(null)));
        assertThat(obj.byteArray(), is(equalTo(null)));
        assertThat(obj.charArray(), is(equalTo(null)));
        assertThat(obj.booleanArray(), is(equalTo(null)));
    }


    @Serialize
    record PrimitiveArrayTypes(
            int[] intArray,
            short[] shortArray,
            long[] longArray,
            float[] floatArray,
            double[] doubleArray,
            byte[] byteArray,
            char[] charArray,
            boolean[] booleanArray
    ) {
    }
}
