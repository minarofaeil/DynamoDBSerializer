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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

/**
 * Serializes and deserializes objects of type {@code T} to and from
 * a {@code Map<String, AttributeValue>}. Implementations define how
 * instances of {@code T} are represented using attribute values.
 *
 * @param <T> the type handled by this serializer
 *
 * @author Mina Rofaeil
 */
public interface Serializer<T> {

    /**
     * Converts the given object into a map representation.
     *
     * @param object the object to serialize; must not be {@code null}
     * @return a map containing the serialized form of the object
     */
    Map<String, AttributeValue> serialize(T object);

    /**
     * Reconstructs an object from its serialized map representation.
     *
     * @param map the map containing serialized attribute values; must not be {@code null}
     * @return the deserialized object
     */
    T deserialize(Map<String, AttributeValue> map);
}
