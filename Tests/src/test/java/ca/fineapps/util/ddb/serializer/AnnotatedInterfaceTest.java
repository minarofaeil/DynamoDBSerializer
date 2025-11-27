package ca.fineapps.util.ddb.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class AnnotatedInterfaceTest {
    private Serializer<TestType> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new ca.fineapps.util.ddb.serializer.AnnotatedInterfaceTest_TestTypeSerializer();
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

        assertThat(testType.getStringValue(), is(equalTo("Test-String")));
        assertThat(testType.getIntValue(), is(equalTo(3)));
    }

    @Serialize(TestType.class)
    private interface TestSerializerInterface {
    }

    static class TestType {
        private String stringValue;
        private int intValue;

        public TestType() {
        }

        public TestType(String stringValue, int intValue) {
            this.stringValue = stringValue;
            this.intValue = intValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }
    }
}
