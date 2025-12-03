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

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class AnnotatedRecordTest {
    private Serializer<TestType> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.AnnotatedRecordTest_TestTypeSerializer();
    }

    @Test
    public void testSerialize() {
        TestType testType = new TestType("Test-String", 3);
        Map<String, AttributeValue> serialized = serializer.serialize(testType);

        assertThat(serialized, hasEntry("stringValue", AttributeValue.builder().s("Test-String").build()));
        assertThat(serialized, hasEntry("intValue", AttributeValue.builder().n("3").build()));
    }

    @Test
    public void testDeserialize() {
        Map<String, AttributeValue> serialized = Map.of(
                "stringValue", AttributeValue.builder().s("Test-String").build(),
                "intValue", AttributeValue.builder().n("3").build()
        );
        TestType testType = serializer.deserialize(serialized);

        assertThat(testType.stringValue(), is(equalTo("Test-String")));
        assertThat(testType.intValue(), is(equalTo(3)));
    }

    @Serialize
    record TestType(String stringValue, int intValue) {
    }
}
