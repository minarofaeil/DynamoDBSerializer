package ca.fineapps.util.ddb.serializer;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Objects;

class EquatableTypeMirror {
    private final Types typeUtil;
    private final TypeMirror type;

    public EquatableTypeMirror(Types typeUtil, TypeMirror type) {
        Objects.requireNonNull(typeUtil);
        Objects.requireNonNull(type);

        this.typeUtil = typeUtil;
        this.type = type;
    }

    public TypeMirror getType() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EquatableTypeMirror)) {
            return false;
        }

        TypeMirror otherType = ((EquatableTypeMirror) other).type;
        return typeUtil.isSameType(type, otherType);
    }

    @Override
    public int hashCode() {
        return type.toString().hashCode();
    }
}
