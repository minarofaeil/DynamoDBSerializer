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
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class CollectionsTest {

    private Serializer<CollectionTypes> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.CollectionsTest_CollectionTypesSerializer();
    }

    @Test
    public void testSerializeCollections() {
        CollectionTypes original = new CollectionTypes(
                List.of(42, 43),
                List.of((short) 7, (short) 8),
                List.of(123456789L, 987654321L),
                List.of(1.23f, 4.56f),
                List.of(7.89, 0.12),
                List.of('x', 'y'),
                List.of((byte) 3, (byte) 4),
                Set.of(true, false),
                Set.of("a", "b")
        );

        Map<String, AttributeValue> map = serializer.serialize(original);

        assertThat(map.get("intList").ns(), contains("42", "43"));
        assertThat(map.get("shortList").ns(), contains("7", "8"));
        assertThat(map.get("longList").ns(), contains("123456789", "987654321"));
        assertThat(map.get("floatList").ns(), contains("1.23", "4.56"));
        assertThat(map.get("doubleList").ns(), contains("7.89", "0.12"));
        assertThat(map.get("byteList").ns(), contains("3", "4"));

        String charList = map.get("charList").s();
        assertThat(charList, is(equalTo("xy")));

        List<AttributeValue> boolList = map.get("booleanSet").l();
        assertThat(boolList, containsInAnyOrder(AttributeValue.fromBool(true), AttributeValue.fromBool(false)));

        assertThat(map.get("stringSet").ss(), containsInAnyOrder("a", "b"));
    }

    @Test
    public void testDeserializeCollections() {
        Map<String, AttributeValue> map = new HashMap<>();

        map.put("intList", AttributeValue.builder().ns("42", "43").build());
        map.put("shortList", AttributeValue.builder().ns("7", "8").build());
        map.put("longList", AttributeValue.builder().ns("123456789", "987654321").build());
        map.put("floatList", AttributeValue.builder().ns("1.23", "4.56").build());
        map.put("doubleList", AttributeValue.builder().ns("7.89", "0.12").build());
        map.put("charList", AttributeValue.builder().s("xy").build());
        map.put("byteList", AttributeValue.builder().ns("3", "4").build());

        map.put("booleanSet", AttributeValue.builder()
                .l(
                        AttributeValue.builder().bool(true).build(),
                        AttributeValue.builder().bool(false).build()
                ).build());

        map.put("stringSet", AttributeValue.builder().ss("a", "b").build());

        CollectionTypes obj = serializer.deserialize(map);

        assertThat(obj.intList(), contains(42, 43));
        assertThat(obj.shortList(), contains((short) 7, (short) 8));
        assertThat(obj.longList(), contains(123456789L, 987654321L));
        assertThat(obj.floatList(), contains(1.23f, 4.56f));
        assertThat(obj.doubleList(), contains(7.89, 0.12));
        assertThat(obj.charList(), contains('x', 'y'));
        assertThat(obj.byteList(), contains((byte) 3, (byte) 4));
        assertThat(obj.booleanSet(), containsInAnyOrder(true, false));
        assertThat(obj.stringSet(), containsInAnyOrder("a", "b"));
    }

    @Test
    public void testSerializeCollectionsDefaults() {
        CollectionTypes original = new CollectionTypes(
                null,
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

        assertThat(map.containsKey("intList"), is(false));
        assertThat(map.containsKey("shortList"), is(false));
        assertThat(map.containsKey("longList"), is(false));
        assertThat(map.containsKey("floatList"), is(false));
        assertThat(map.containsKey("doubleList"), is(false));
        assertThat(map.containsKey("charList"), is(false));
        assertThat(map.containsKey("byteList"), is(false));
        assertThat(map.containsKey("booleanSet"), is(false));
        assertThat(map.containsKey("stringSet"), is(false));
    }

    @Test
    public void testDeserializeCollectionsDefaults() {
        Map<String, AttributeValue> map = new HashMap<>();

        CollectionTypes obj = serializer.deserialize(map);

        assertThat(obj.intList(), is(equalTo(null)));
        assertThat(obj.shortList(), is(equalTo(null)));
        assertThat(obj.longList(), is(equalTo(null)));
        assertThat(obj.floatList(), is(equalTo(null)));
        assertThat(obj.doubleList(), is(equalTo(null)));
        assertThat(obj.charList(), is(equalTo(null)));
        assertThat(obj.byteList(), is(equalTo(null)));
        assertThat(obj.booleanSet(), is(equalTo(null)));
        assertThat(obj.stringSet(), is(equalTo(null)));
    }

    @Serialize
    record CollectionTypes(
            List<Integer> intList,
            List<Short> shortList,
            List<Long> longList,
            List<Float> floatList,
            List<Double> doubleList,
            List<Character> charList,
            List<Byte> byteList,
            Set<Boolean> booleanSet,
            Set<String> stringSet
    ) {
    }
}
