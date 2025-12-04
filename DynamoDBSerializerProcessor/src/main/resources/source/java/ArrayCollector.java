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

import javax.annotation.processing.Generated;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;

@Generated(value = "ca.fineapps.util.ddb.serializer.DynamoDBSerializerProcessor")
class ArrayCollector<T, R> implements Collector<T, List<T>, R> {
    private final IntFunction<R> arrayFactory;

    public ArrayCollector(IntFunction<R> arrayFactory) {
        this.arrayFactory = arrayFactory;
    }

    @Override
    public Supplier<List<T>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<T>, T> accumulator() {
        return List::add;
    }

    @Override
    public BinaryOperator<List<T>> combiner() {
        return (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
    }

    @Override
    public Function<List<T>, R> finisher() {
        return list -> {
            R array = arrayFactory.apply(list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }
}
