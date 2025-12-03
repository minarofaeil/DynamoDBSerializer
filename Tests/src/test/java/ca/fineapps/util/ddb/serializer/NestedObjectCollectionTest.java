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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class NestedObjectCollectionTest {

    private Serializer<HasNestedObject> serializer;

    @BeforeEach
    public void setUp() {
        serializer = ca.fineapps.util.ddb.serializer.NestedObjectCollectionTest_HasNestedObjectSerializer.create();
    }

    @Test
    public void testSerialize() {
        NestedObject nested1 = new NestedObject("nested-1", 100);
        NestedObject nested2 = new NestedObject("nested-2", 200);

        List<NestedObject> list = new ArrayList<>();
        list.add(nested1);
        list.add(nested2);

        Set<NestedObject> set = new HashSet<>();
        set.add(nested1);
        set.add(nested2);

        HasNestedObject original = new HasNestedObject(list, set);

        Map<String, AttributeValue> map = serializer.serialize(original);

        AttributeValue listAttribute = map.get("nestedObjectsList");
        List<AttributeValue> listSerialized = listAttribute.l();
        assertThat(listSerialized.size(), is(equalTo(2)));

        Map<String, AttributeValue> listFirst = listSerialized.get(0).m();
        Map<String, AttributeValue> listSecond = listSerialized.get(1).m();

        assertThat(listFirst.get("stringValue").s(), is(equalTo("nested-1")));
        assertThat(listFirst.get("intValue").n(), is(equalTo("100")));

        assertThat(listSecond.get("stringValue").s(), is(equalTo("nested-2")));
        assertThat(listSecond.get("intValue").n(), is(equalTo("200")));

        // Set serialization (order may not be guaranteed, so allow both orders)
        AttributeValue setAttr = map.get("nestedObjectSet");
        List<AttributeValue> setSerialized = setAttr.l();
        assertThat(setSerialized.size(), is(equalTo(2)));

        Map<String, AttributeValue> setFirst = setSerialized.get(0).m();
        Map<String, AttributeValue> setSecond = setSerialized.get(1).m();

        String firstString = setFirst.get("stringValue").s();
        String secondString = setSecond.get("stringValue").s();

        if ("nested-1".equals(firstString)) {
            // First is nested-1, second is nested-2
            assertThat(setFirst.get("intValue").n(), is(equalTo("100")));
            assertThat(secondString, is(equalTo("nested-2")));
            assertThat(setSecond.get("intValue").n(), is(equalTo("200")));
        } else {
            // First is nested-2, second is nested-1
            assertThat(firstString, is(equalTo("nested-2")));
            assertThat(setFirst.get("intValue").n(), is(equalTo("200")));
            assertThat(secondString, is(equalTo("nested-1")));
            assertThat(setSecond.get("intValue").n(), is(equalTo("100")));
        }
    }

    @Test
    public void testDeserialize() {
        // For list
        Map<String, AttributeValue> nestedMap1 = new HashMap<>();
        nestedMap1.put("stringValue", AttributeValue.builder().s("nested-1").build());
        nestedMap1.put("intValue", AttributeValue.builder().n("100").build());

        Map<String, AttributeValue> nestedMap2 = new HashMap<>();
        nestedMap2.put("stringValue", AttributeValue.builder().s("nested-2").build());
        nestedMap2.put("intValue", AttributeValue.builder().n("200").build());

        List<AttributeValue> listSerialized = new ArrayList<>();
        listSerialized.add(AttributeValue.builder().m(nestedMap1).build());
        listSerialized.add(AttributeValue.builder().m(nestedMap2).build());

        List<AttributeValue> setSerialized = new ArrayList<>();
        setSerialized.add(AttributeValue.builder().m(nestedMap1).build());
        setSerialized.add(AttributeValue.builder().m(nestedMap2).build());

        Map<String, AttributeValue> map = new HashMap<>();
        map.put("nestedObjectsList", AttributeValue.builder().l(listSerialized).build());
        map.put("nestedObjectSet", AttributeValue.builder().l(setSerialized).build());

        HasNestedObject original = serializer.deserialize(map);

        List<NestedObject> nestedObjectsList = original.nestedObjectsList();
        assertThat(nestedObjectsList, is(not(nullValue())));
        assertThat(nestedObjectsList.size(), is(equalTo(2)));

        assertThat(nestedObjectsList.get(0).stringValue(), is(equalTo("nested-1")));
        assertThat(nestedObjectsList.get(0).intValue(), is(equalTo(100)));

        assertThat(nestedObjectsList.get(1).stringValue(), is(equalTo("nested-2")));
        assertThat(nestedObjectsList.get(1).intValue(), is(equalTo(200)));

        Set<NestedObject> nestedObjectSet = original.nestedObjectSet();
        assertThat(nestedObjectSet, is(not(nullValue())));
        assertThat(nestedObjectSet.size(), is(equalTo(2)));

        NestedObject expected1 = new NestedObject("nested-1", 100);
        NestedObject expected2 = new NestedObject("nested-2", 200);

        assertThat(nestedObjectSet, containsInAnyOrder(expected1, expected2));
    }

    @Test
    public void testDeserializeWithoutNestedObjects() {
        Map<String, AttributeValue> map = new HashMap<>();

        HasNestedObject original = serializer.deserialize(map);

        assertThat(original.nestedObjectsList(), is(nullValue()));
        assertThat(original.nestedObjectSet(), is(nullValue()));
    }

    @Serialize
    record HasNestedObject(
            List<NestedObject> nestedObjectsList,
            Set<NestedObject> nestedObjectSet
    ) {
    }

    record NestedObject(
            String stringValue,
            int intValue
    ) {
    }
}
