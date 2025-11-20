package ca.fineapps.util.ddb.serializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.StringWriter;
import java.util.List;

class FieldSerializer {
    private final Types typeUtils;
    private final DynamoDBTypeMapper typeMapper;

    FieldSerializer(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        typeMapper = new DynamoDBTypeMapper(typeUtils, elementUtils);
    }

    void generateFieldSerialization(StringWriter writer, TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                AttributeValue.Type ddbType = typeMapper.findDynamoDBType(enclosedElement.asType());
                String getter = findGetter(element, enclosedElement);

                if (getter != null && ddbType != null) {
                    writer.write("\t\tmap.put(\"" + enclosedElement.getSimpleName() + "\", " +
                            "AttributeValue.from" + ddbType + "(String.valueOf(object." + getter + "()))" +
                            ");\n");
                }
            }
        }
    }

    private String findGetter(TypeElement type, Element field) {
        String fieldName = field.getSimpleName().toString();

        List<? extends Element> enclosedElements = type.getEnclosedElements();
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                // Getter has same name as field, for example in records.
                if (enclosedElement.getSimpleName().toString().equals(fieldName)) {
                    return fieldName;
                }

                // Getter follows standard Java bean getter name.
                String getterMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                if (enclosedElement.getSimpleName().toString().equals(getterMethodName)) {
                    return getterMethodName;
                }

                // Boolean-style Java bean getter name.
                String booleanGetterMethodName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                if (enclosedElement.getSimpleName().toString().equals(booleanGetterMethodName)) {
                    return booleanGetterMethodName;
                }
            }
        }

        return null;
    }
}
