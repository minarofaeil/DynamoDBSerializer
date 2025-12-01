package ca.fineapps.util.ddb.serializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

class NameUtils {
    private final Types typeUtils;

    NameUtils(Types typeUtils) {
        this.typeUtils = typeUtils;
    }

    String serializerClassName(TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        Element enclosing = element.getEnclosingElement();
        String enclosingTypeName = enclosing instanceof TypeElement ? enclosing.getSimpleName() + "_" : "";
        return enclosingTypeName + element.getSimpleName().toString() + "Serializer";
    }

    String camelCase(String className) {
        return Character.toLowerCase(className.charAt(0)) +
                (className.length() > 1 ? className.substring(1) : "");
    }
}
