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
