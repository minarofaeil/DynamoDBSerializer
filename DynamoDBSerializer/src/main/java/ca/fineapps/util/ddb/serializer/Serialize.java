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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a POJO that follows standard JavaBeans conventions or a record with
 * {@link Serialize} to generate a corresponding {@link Serializer} implementation.
 *
 * <p>For a class named {@code MyDataType}, the generated implementation will be
 * {@code MyDataTypeSerializer}, created in the same package as {@code MyDataType}.
 * An instance can be obtained as follows:
 * </p>
 *
 * <pre>
 * {@link Serializer}&lt;MyDataType&gt; serializer = MyDataTypeSerializer.create();
 * </pre>
 *
 * <p>If {@code MyDataType} cannot be modified, apply {@link Serialize} to an empty
 * interface that references the class:
 * </p>
 *
 * <pre>
 * {@literal @}Serialize(MyDataType.class)
 * public interface MyDataTypeSerializerProvider {
 * }
 * </pre>
 *
 * <p>Do not name the interface {@code MyDataTypeSerializer}, as that name is reserved
 * for the generated {@link Serializer} implementation. The resulting serializer is used
 * in the same way as shown above.
 * </p>
 *
 * @see Serializer
 * @author Mina Rofaeil
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Serialize {
    Class<?> value() default void.class;
}
