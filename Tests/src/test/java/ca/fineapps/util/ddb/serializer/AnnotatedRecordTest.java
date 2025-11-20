package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class AnnotatedRecordTest {

    @Test
    public void testSerialize() {
        TestType testType = new TestType("Test-String", 3);
        Serializer<TestType> serializer = new ca.fineapps.util.ddb.serializer.AnnotatedRecordTest_TestTypeSerializer();

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

        Serializer<TestType> serializer = new ca.fineapps.util.ddb.serializer.AnnotatedRecordTest_TestTypeSerializer();
        TestType testType = serializer.deserialize(serialized);

        assertThat(testType.stringValue(), is(equalTo("Test-String")));
        assertThat(testType.intValue(), is(equalTo(3)));
    }

    @Serialize
    record TestType(String stringValue, int intValue) {
    }
}
