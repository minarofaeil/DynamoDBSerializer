package ca.fineapps.util.ddb.serializer;

import java.util.function.IntFunction;
import java.util.stream.Collector;

public class Collectors {
    private Collectors() {
    }

    public static <T, R> Collector<T, ?, R> toArray(IntFunction<R> arrayFactory) {
        return new ArrayCollector<>(arrayFactory);
    }
}
