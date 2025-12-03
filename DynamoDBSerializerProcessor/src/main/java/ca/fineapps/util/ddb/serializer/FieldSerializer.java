package ca.fineapps.util.ddb.serializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

class FieldSerializer {
    private final Types typeUtils;
    private final DynamoDBTypeMapper typeMapper;
    private final NameUtils nameUtils;
    private final Map<String, BiFunction<TypeMirror, String, String>> customSerializers;

    FieldSerializer(Types typeUtils, Elements elementUtils, NameUtils nameUtils) {
        this.typeUtils = typeUtils;
        this.typeMapper = new DynamoDBTypeMapper(typeUtils, elementUtils);
        this.nameUtils = nameUtils;
        this.customSerializers = buildCustomSerializers();
    }

    void generateFieldSerialization(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                TypeMirror elementType = enclosedElement.asType();
                AttributeValue.Type ddbType = typeMapper.findDynamoDBType(elementType);
                String getter = findGetter(element, enclosedElement);

                if (getter != null && ddbType != null) {
                    if (ddbType == AttributeValue.Type.M) {
                        dependencies.add(elementType);
                    }

                    if (!elementType.getKind().isPrimitive()) {
                        writer.write("\t\tif (object." + getter + "() != null) {\n");
                        writer.write("\t");
                    }
                    writer.write("\t\tmap.put(\"" + enclosedElement.getSimpleName() + "\", " +
                            "AttributeValue.from" + camelCase(ddbType) + "(" +
                            wrapGetter(elementType, "object." + getter + "()", dependencies) +
                            "));\n");
                    if (!elementType.getKind().isPrimitive()) {
                        writer.write("\t\t}\n");
                    }
                }
            }
        }
    }

    private String camelCase(AttributeValue.Type ddbType) {
        String typeName = ddbType.name();
        return Character.toUpperCase(typeName.charAt(0)) + (typeName.length() > 1 ? typeName.substring(1).toLowerCase() : "");
    }

    private String wrapGetter(TypeMirror type, String getter, Collection<TypeMirror> dependencies) {
        String template = switch (type.toString()) {
            case "int", "java.lang.Integer",
                 "long", "java.lang.Long",
                 "double", "java.lang.Double",
                 "short", "java.lang.Short",
                 "byte", "java.lang.Byte",
                 "float", "java.lang.Float",
                 "char", "java.lang.Character" -> "String.valueOf(%s)";
            default -> null;
        };

        if (typeMapper.isArray(type)) {
            TypeMirror arrayType = typeMapper.findArrayOrCollectionType(type);
            if (arrayType.toString().equals("byte")) {
                template = "SdkBytes.fromByteArray(%s)";
            } else if (typeMapper.isNumber(arrayType)) {
                template = "java.util.stream.IntStream.range(0, %s.length).mapToObj(i -> %s[i]).map(String::valueOf).toList()";
            } else if (typeMapper.isString(arrayType)) {
                template = "Arrays.asList(%s)";
            } else if (arrayType.toString().equals("char")) {
                template = "new String(%s)";
            } else if (arrayType.toString().equals("java.lang.Character")) {
                template = "new String(Stream.of(%s).map(String::valueOf).collect(java.util.stream.Collectors.joining()))";
            } else if (arrayType.toString().equals("boolean")) {
                template = "java.util.stream.IntStream.range(0, %s.length).mapToObj(i -> AttributeValue.fromBool(%s[i])).toList()";
            } else if (arrayType.toString().equals("java.lang.Boolean")) {
                template = "Arrays.stream(%s).map(AttributeValue::fromBool).toList()";
            } else if (typeMapper.findDynamoDBType(arrayType) == AttributeValue.Type.M) {
                dependencies.add(arrayType);
                template = "Arrays.stream(%s)\n" +
                        "\t\t\t\t\t.map(item -> " + customSerializer(arrayType, "item") + ")\n" +
                        "\t\t\t\t\t.map(AttributeValue::fromM)\n" +
                        "\t\t\t\t\t.toList()\n" +
                        "\t\t\t";
            }
        } else if (typeMapper.isCollection(type)) {
            TypeMirror itemType = typeMapper.findArrayOrCollectionType(type);
            if (itemType.toString().equals("byte")) {
                template = "SdkBytes.fromByteArray(%s)";
            } else if (typeMapper.isNumber(itemType)) {
                template = "%s.stream().map(String::valueOf).toList()";
            } else if (typeMapper.isString(itemType)) {
                template = "new java.util.ArrayList<>(%s)";
            } else if (itemType.toString().equals("java.lang.Character")) {
                template = "%s.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining())";
            } else if (itemType.toString().equals("java.lang.Boolean")) {
                template = "%s.stream().map(AttributeValue::fromBool).toList()";
            } else if (typeMapper.findDynamoDBType(itemType) == AttributeValue.Type.M) {
                dependencies.add(itemType);
                template = "%s.stream()\n" +
                        "\t\t\t\t\t.map(item -> " + customSerializer(itemType, "item") + ")\n" +
                        "\t\t\t\t\t.map(AttributeValue::fromM)\n" +
                        "\t\t\t\t\t.toList()\n" +
                        "\t\t\t";
            }
        } else if (customSerializers.containsKey(type.toString()) ||
                typeMapper.findDynamoDBType(type) == AttributeValue.Type.M) {
            template = customSerializer(type, "%s");
        }

        return template == null ? getter : String.format(template, getter, getter);
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

    private Map<String, BiFunction<TypeMirror, String, String>> buildCustomSerializers() {
        Map<String, BiFunction<TypeMirror, String, String>> map = new HashMap<>();

        map.put("java.time.Instant", (instanceType, getter) -> "String.valueOf(" + getter + ".toEpochMilli())");

        return map;
    }

    private String customSerializer(TypeMirror type, String getter) {
        BiFunction<TypeMirror, String, String> serializer = customSerializers.getOrDefault(type.toString(),
                (aType, aGetter) ->
                        nameUtils.camelCase(nameUtils.serializerClassName(aType)) + ".serialize(" + aGetter + ")"
        );
        return serializer.apply(type, getter);
    }
}
