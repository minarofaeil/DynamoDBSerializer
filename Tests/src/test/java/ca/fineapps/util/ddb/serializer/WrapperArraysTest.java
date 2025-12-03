/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class WrapperArraysTest {

    private Serializer<WrapperArrayTypes> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.WrapperArraysTest_WrapperArrayTypesSerializer();
    }

    @Test
    public void testSerializeWrapperArray() {
        WrapperArrayTypes original = new WrapperArrayTypes(
                new Integer[] {42, 43},
                new Short[] {7, 8},
                new Long[] {123456789L, 987654321L},
                new Float[] {1.23f, 4.56f},
                new Double[] {7.89, 0.12},
                new Byte[] {3, 4},
                new Character[] {'x', 'y'},
                new Boolean[] {true, false}
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        List<String> intList = map.get("intArray").ns();
        assertThat(intList.get(0), is(equalTo("42")));
        assertThat(intList.get(1), is(equalTo("43")));

        List<String> shortList = map.get("shortArray").ns();
        assertThat(shortList.get(0), is(equalTo("7")));
        assertThat(shortList.get(1), is(equalTo("8")));

        List<String> longList = map.get("longArray").ns();
        assertThat(longList.get(0), is(equalTo("123456789")));
        assertThat(longList.get(1), is(equalTo("987654321")));

        List<String> floatList = map.get("floatArray").ns();
        assertThat(floatList.get(0), is(equalTo(Float.toString(1.23f))));
        assertThat(floatList.get(1), is(equalTo(Float.toString(4.56f))));

        List<String> doubleList = map.get("doubleArray").ns();
        assertThat(doubleList.get(0), is(equalTo(Double.toString(7.89))));
        assertThat(doubleList.get(1), is(equalTo(Double.toString(0.12))));

        List<String> byteList = map.get("byteArray").ns();
        assertThat(byteList.get(0), is(equalTo("3")));
        assertThat(byteList.get(1), is(equalTo("4")));

        String charList = map.get("charArray").s();
        assertThat(charList.charAt(0), is(equalTo('x')));
        assertThat(charList.charAt(1), is(equalTo('y')));

        List<AttributeValue> boolList = map.get("booleanArray").l();
        assertThat(boolList.get(0).bool(), is(equalTo(true)));
        assertThat(boolList.get(1).bool(), is(equalTo(false)));
    }

    @Test
    public void testDeserializeWrapperArray() {
        Map<String, AttributeValue> map = new HashMap<>();

        map.put("intArray", AttributeValue.builder().ns("42", "43").build());
        map.put("shortArray", AttributeValue.builder().ns("7", "8").build());
        map.put("longArray", AttributeValue.builder().ns("123456789", "987654321").build());
        map.put("floatArray", AttributeValue.builder().ns("1.23", "4.56f").build());
        map.put("doubleArray", AttributeValue.builder().ns("7.89", "0.12").build());
        map.put("byteArray", AttributeValue.builder().ns("3", "4").build());
        map.put("charArray", AttributeValue.builder().s("xy").build());
        map.put("booleanArray", AttributeValue.builder()
                .l(AttributeValue.fromBool(true), AttributeValue.fromBool(false))
                .build()
        );

        WrapperArrayTypes obj = serializer.deserialize(map);

        assertThat(obj.intArray()[0], is(equalTo(42)));
        assertThat(obj.intArray()[1], is(equalTo(43)));

        assertThat(obj.shortArray()[0], is(equalTo((short) 7)));
        assertThat(obj.shortArray()[1], is(equalTo((short) 8)));

        assertThat(obj.longArray()[0], is(equalTo(123456789L)));
        assertThat(obj.longArray()[1], is(equalTo(987654321L)));

        assertThat(obj.floatArray()[0], is(equalTo(1.23f)));
        assertThat(obj.floatArray()[1], is(equalTo(4.56f)));

        assertThat(obj.doubleArray()[0], is(equalTo(7.89)));
        assertThat(obj.doubleArray()[1], is(equalTo(0.12)));

        assertThat(obj.byteArray()[0], is(equalTo((byte) 3)));
        assertThat(obj.byteArray()[1], is(equalTo((byte) 4)));

        assertThat(obj.charArray()[0], is(equalTo('x')));
        assertThat(obj.charArray()[1], is(equalTo('y')));

        assertThat(obj.booleanArray()[0], is(equalTo(true)));
        assertThat(obj.booleanArray()[1], is(equalTo(false)));
    }

    @Test
    public void testSerializeWrapperArrayDefaults() {
        WrapperArrayTypes original = new WrapperArrayTypes(
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
    public void testDeserializeWrapperArrayDefaults() {
        Map<String, AttributeValue> map = new HashMap<>();

        WrapperArrayTypes obj = serializer.deserialize(map);

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
    record WrapperArrayTypes(
            Integer[] intArray,
            Short[] shortArray,
            Long[] longArray,
            Float[] floatArray,
            Double[] doubleArray,
            Byte[] byteArray,
            Character[] charArray,
            Boolean[] booleanArray
    ) {
    }
}
