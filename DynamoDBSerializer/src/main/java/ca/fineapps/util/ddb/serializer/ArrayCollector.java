package ca.fineapps.util.ddb.serializer;

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
