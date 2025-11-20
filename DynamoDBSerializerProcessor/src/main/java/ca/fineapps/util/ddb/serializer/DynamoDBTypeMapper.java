package ca.fineapps.util.ddb.serializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

class DynamoDBTypeMapper {
    private final Types typeUtils;
    private final Elements elementUtils;

    public DynamoDBTypeMapper(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
    }

    AttributeValue.Type findDynamoDBType(TypeMirror type) {
        String typeName = type.toString();

        switch (typeName) {
            case "int":
            case "java.lang.Integer":
            case "long":
            case "java.lang.Long":
            case "double":
            case "java.lang.Double":
            case "float":
            case "java.lang.Float":
                return AttributeValue.Type.N;

            case "boolean":
            case "java.lang.Boolean":
                return AttributeValue.Type.B;

            case "java.lang.String":
                return AttributeValue.Type.S;
        }

        if (typeName.endsWith("[]") || typeUtils.isSubtype(
                typeUtils.erasure(type),
                typeUtils.erasure(elementUtils.getTypeElement("java.util.Collection").asType()))
        ) {
            return AttributeValue.Type.L;
        }

        return null;
    }
}
