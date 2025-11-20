package ca.fineapps.util.ddb.serializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface Serializer<T> {

    Map<String, AttributeValue> serialize(T object);

    T deserialize(Map<String, AttributeValue> map);
}
