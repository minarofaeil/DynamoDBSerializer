package ca.fineapps.util.ddb.serializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Set;

class DynamoDBTypeMapper {
    private final Types typeUtils;
    private final Elements elementUtils;

    public DynamoDBTypeMapper(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
    }

    AttributeValue.Type findDynamoDBType(TypeMirror type) {
        if (isNumber(type)) {
            return AttributeValue.Type.N;
        }

        String typeName = type.toString();
        switch (typeName) {
            case "boolean":
            case "java.lang.Boolean":
                return AttributeValue.Type.BOOL;

            case "char":
            case "java.lang.Character":
            case "java.lang.String":
                return AttributeValue.Type.S;

            case "byte[]":
                return AttributeValue.Type.B;
        }

        if (isArrayOrCollection(type)) {
            TypeMirror entityType = findArrayOrCollectionType(type);

            if (isNumber(entityType)) {
                return AttributeValue.Type.NS;
            } else if (isString(entityType)) {
                return AttributeValue.Type.SS;
            } else if (isChar(entityType)) {
                return AttributeValue.Type.S;
            } else {
                return AttributeValue.Type.L;
            }
        }

        return AttributeValue.Type.M;
    }

    boolean isArrayOrCollection(TypeMirror type) {
        return isArray(type) || isCollection(type);
    }

    boolean isArray(TypeMirror type) {
        return type.getKind() == TypeKind.ARRAY;
    }

    boolean isCollection(TypeMirror type) {
        return typeUtils.isSubtype(
                typeUtils.erasure(type),
                typeUtils.erasure(elementUtils.getTypeElement("java.util.Collection").asType())
        );
    }

    TypeMirror findArrayOrCollectionType(TypeMirror type) {
        TypeMirror entityType;
        if (type.getKind() == TypeKind.ARRAY) {
            entityType = ((ArrayType) type).getComponentType();
        } else {
            entityType = ((DeclaredType) type).getTypeArguments().getFirst();
        }
        return entityType;
    }

    boolean isNumber(TypeMirror type) {
        Set<String> numericTypes = Set.of(
                "int",
                "java.lang.Integer",
                "short",
                "java.lang.Short",
                "long",
                "java.lang.Long",
                "double",
                "java.lang.Double",
                "float",
                "java.lang.Float",
                "byte",
                "java.lang.Byte"
        );

        if (numericTypes.contains(type.toString())) {
            return true;
        }

        return typeUtils.isSubtype(
                typeUtils.erasure(type),
                typeUtils.erasure(elementUtils.getTypeElement("java.lang.Number").asType())
        );
    }

    boolean isString(TypeMirror type) {
        return typeUtils.isSubtype(
                typeUtils.erasure(type),
                typeUtils.erasure(elementUtils.getTypeElement("java.lang.CharSequence").asType())
        );
    }

    boolean isChar(TypeMirror type) {
        return Set.of(
                "char",
                "java.lang.Character"
        ).contains(type.toString());
    }

    boolean isSet(TypeMirror type) {
        return typeUtils.isSubtype(
                typeUtils.erasure(type),
                typeUtils.erasure(elementUtils.getTypeElement("java.util.Set").asType())
        );
    }
}
