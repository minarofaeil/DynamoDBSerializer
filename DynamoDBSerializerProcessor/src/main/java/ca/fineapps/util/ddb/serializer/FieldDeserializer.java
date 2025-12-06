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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
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
import java.util.stream.Collectors;

class FieldDeserializer {
    private final Types typeUtils;
    private final DynamoDBTypeMapper typeMapper;
    private final NameUtils nameUtils;
    private final Map<String, BiFunction<TypeMirror, String, String>> customDeserializers;

    FieldDeserializer(Types typeUtils, Elements elementUtils, NameUtils nameUtils) {
        this.typeUtils = typeUtils;
        this.typeMapper = new DynamoDBTypeMapper(typeUtils, elementUtils);
        this.nameUtils = nameUtils;
        this.customDeserializers = buildCustomDeserializers();
    }

    void generateFieldDeserialization(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        Element element = typeUtils.asElement(type);
        Constructor constructor = findConstructor(element);

        if (constructor != null) {
            if (constructor.isNoArgs()) {
                writer.write("\t\t" + element.getSimpleName() + " object = new " + element.getSimpleName() + "();\n");
                writer.write("\n");
                generateFieldDeserializationWithSetters(type, writer, dependencies);
                writer.write("\t\treturn object;\n");
            } else {
                writer.write("\t\treturn new " + element.getSimpleName() + "(\n");
                generateFieldDeserializationWithConstructorParameters(constructor, writer, dependencies);
                writer.write("\t\t);\n");
            }
        }
    }

    private void generateFieldDeserializationWithSetters(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        Element element = typeUtils.asElement(type);
        List<? extends Element> enclosedElements = element.getEnclosedElements();

        // Find setters. Protected and package-private setters are fine because the serializer is generated in the same
        // package as the serialized type
        for (Element enclosedElement : enclosedElements) {
            String enclosedElementName = enclosedElement.getSimpleName().toString();

            if (enclosedElement.getKind() == ElementKind.METHOD &&
                    !enclosedElement.getModifiers().contains(Modifier.PRIVATE) &&
                    enclosedElementName.startsWith("set") && enclosedElementName.length() > 3) {

                ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                List<? extends VariableElement> parameters = executableElement.getParameters();

                if (parameters.size() == 1) {
                    TypeMirror paramType = parameters.getFirst().asType();
                    AttributeValue.Type ddbType = typeMapper.findDynamoDBType(paramType);
                    if (ddbType == AttributeValue.Type.M) {
                        dependencies.add(paramType);
                    }

                    String fieldName = Character.toLowerCase(enclosedElementName.charAt(3)) +
                            (enclosedElementName.length() > 4 ? enclosedElementName.substring(4) : "");
                    writer.write("\t\tif (map.containsKey(\"" + fieldName + "\")) {\n");

                    String mapGetter = "map.get(\"" + fieldName + "\")." + ddbType.name().toLowerCase() + "()";
                    mapGetter = wrapMapGetter(paramType, mapGetter);

                    writer.write("\t\t\tobject." + enclosedElement.getSimpleName() + "(" +
                            mapGetter +
                            ");\n");
                    writer.write("\t\t}\n");
                    writer.write("\n");
                }
            }
        }
    }

    private void generateFieldDeserializationWithConstructorParameters(Constructor constructor, Writer writer,
            Collection<TypeMirror> dependencies) throws IOException {
        writer.write(constructor.args().stream()
                .peek(param -> {
                    if (param.ddbType() == AttributeValue.Type.M) {
                        dependencies.add(param.type());
                    }
                })
                .map(this::mapConstructorArg)
                .collect(Collectors.joining(",\n")) + "\n"
        );
    }

    private String mapConstructorArg(Param param) {
        return "\t\t\t\tmap.containsKey(\"" + param.name() + "\") ? " +
                wrapMapGetter(param.type(),
                        "map.get(\"" + param.name() + "\")." +
                                param.ddbType().name().toLowerCase() + "()"
                ) + " : " +
                defaultValue(param);
    }

    private static String defaultValue(Param arg) {
        TypeKind kind = arg.type().getKind();
        if (kind.isPrimitive()) {
            if (kind == TypeKind.BOOLEAN) {
                return "false";
            } else {
                return "0";
            }
        } else {
            return "null";
        }
    }

    private String wrapMapGetter(TypeMirror type, String mapGetter) {
        String template = switch (type.toString()) {
            case "int", "java.lang.Integer" -> "Integer.parseInt(%s)";
            case "long", "java.lang.Long" -> "Long.parseLong(%s)";
            case "double", "java.lang.Double" -> "Double.parseDouble(%s)";
            case "short", "java.lang.Short" -> "Short.parseShort(%s)";
            case "byte", "java.lang.Byte" -> "Byte.parseByte(%s)";
            case "float", "java.lang.Float" -> "Float.parseFloat(%s)";
            case "char", "java.lang.Character" -> "%s.charAt(0)";
            default -> null;
        };

        if (typeMapper.isEnum(type)) {
            template = ((TypeElement) typeUtils.asElement(type)).getQualifiedName() + ".valueOf(%s)";
        } else if (typeMapper.isArray(type)) {
            TypeMirror arrayType = typeMapper.findArrayOrCollectionType(type);
            if (arrayType.toString().equals("byte")) {
                template = "%s.asByteArray()";
            } else if (typeMapper.isNumber(arrayType)) {
                template = switch (arrayType.toString()) {
                    case "int" -> "%s.stream().map(Integer::parseInt)\n\t\t\t\t\t\t" +
                            ".collect(ca.fineapps.util.ddb.serializer.Collectors.toArray(int[]::new))";
                    case "short" -> "%s.stream().map(Short::parseShort)\n\t\t\t\t\t\t" +
                            ".collect(ca.fineapps.util.ddb.serializer.Collectors.toArray(short[]::new))";
                    case "long" -> "%s.stream().map(Long::parseLong)\n\t\t\t\t\t\t" +
                            ".collect(ca.fineapps.util.ddb.serializer.Collectors.toArray(long[]::new))";
                    case "float" -> "%s.stream().map(Float::parseFloat)\n\t\t\t\t\t\t" +
                            ".collect(ca.fineapps.util.ddb.serializer.Collectors.toArray(float[]::new))";
                    case "double" -> "%s.stream().map(Double::parseDouble)\n\t\t\t\t\t\t" +
                            ".collect(ca.fineapps.util.ddb.serializer.Collectors.toArray(double[]::new))";

                    case "java.lang.Integer" -> "%s.stream().map(Integer::valueOf).toArray(Integer[]::new)";
                    case "java.lang.Short" -> "%s.stream().map(Short::valueOf).toArray(Short[]::new)";
                    case "java.lang.Long" -> "%s.stream().map(Long::valueOf).toArray(Long[]::new)";
                    case "java.lang.Float" -> "%s.stream().map(Float::valueOf).toArray(Float[]::new)";
                    case "java.lang.Double" -> "%s.stream().map(Double::valueOf).toArray(Double[]::new)";
                    case "java.lang.Byte" -> "%s.stream().map(Byte::valueOf).toArray(Byte[]::new)";
                    default -> null;
                };
            } else if (typeMapper.isString(arrayType)) {
                template = "%s.toArray(String[]::new)";
            } else if (arrayType.toString().equals("char")) {
                template = "%s.toCharArray()";
            } else if (arrayType.toString().equals("java.lang.Character")) {
                template = "java.util.stream.IntStream.range(0, %s.length()).mapToObj(%s::charAt).toArray(Character[]::new)";
            } else if (arrayType.toString().equals("boolean")) {
                template = "%s.stream().map(AttributeValue::bool)\n\t\t\t\t\t\t" +
                        ".collect(ca.fineapps.util.ddb.serializer.Collectors.toArray(boolean[]::new))";
            } else if (arrayType.toString().equals("java.lang.Boolean")) {
                template = "%s.stream().map(AttributeValue::bool).toArray(Boolean[]::new)";
            } else if (typeMapper.findDynamoDBType(arrayType) == AttributeValue.Type.M) {
                template = "%s.stream()\n" +
                        "\t\t\t\t\t.map(AttributeValue::m)\n" +
                        "\t\t\t\t\t.map(item -> " + customDeserializer(arrayType, "item") + ")\n" +
                        "\t\t\t\t\t.toArray(" + arrayType + "[]::new)";
            } else if (typeMapper.isEnum(arrayType)) {
                template = "%s.stream().map(" + ((TypeElement) typeUtils.asElement(arrayType)).getQualifiedName() + "::valueOf)\n" +
                        "\t\t\t\t\t.toArray(" + ((TypeElement) typeUtils.asElement(arrayType)).getQualifiedName() + "[]::new)";
            }
        } else if (typeMapper.isCollection(type)) {
            TypeMirror itemType = typeMapper.findArrayOrCollectionType(type);
            String collector = typeMapper.isSet(type) ? "collect(java.util.stream.Collectors.toSet())" : "toList()";
            if (typeMapper.isNumber(itemType)) {
                template = switch (itemType.toString()) {
                    case "java.lang.Integer" -> "%s.stream().map(Integer::parseInt)." + collector;
                    case "java.lang.Short" -> "%s.stream().map(Short::parseShort)." + collector;
                    case "java.lang.Long" -> "%s.stream().map(Long::parseLong)." + collector;
                    case "java.lang.Float" -> "%s.stream().map(Float::parseFloat)." + collector;
                    case "java.lang.Double" -> "%s.stream().map(Double::parseDouble)." + collector;
                    case "java.lang.Byte" -> "%s.stream().map(Byte::parseByte)." + collector;
                    default -> null;
                };
            } else if (typeMapper.isString(itemType)) {
                if (typeMapper.isSet(type)) {
                    template = "%s.stream()." + collector;
                }
            } else if (itemType.toString().equals("java.lang.Character")) {
                template = "java.util.stream.IntStream.range(0, %s.length()).mapToObj(%s::charAt)." + collector;
            } else if (itemType.toString().equals("java.lang.Boolean")) {
                template = "%s.stream().map(AttributeValue::bool)." + collector;
            } else if (typeMapper.findDynamoDBType(itemType) == AttributeValue.Type.M) {
                template = "%s.stream()\n" +
                        "\t\t\t\t\t.map(AttributeValue::m)\n" +
                        "\t\t\t\t\t.map(item -> " + customDeserializer(itemType, "item") + ")\n" +
                        "\t\t\t\t\t." + collector;
            } else if (typeMapper.isEnum(itemType)) {
                template = "%s.stream().map(" + ((TypeElement) typeUtils.asElement(itemType)).getQualifiedName() + "::valueOf)\n" +
                        "\t\t\t\t\t." + collector;
            }
        } else if (customDeserializers.containsKey(type.toString()) ||
                typeMapper.findDynamoDBType(type) == AttributeValue.Type.M) {
            template = customDeserializer(type, "%s");
        }

        return template == null ? mapGetter : String.format(template, mapGetter, mapGetter);
    }

    private Constructor findConstructor(Element element) {
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        Constructor chosen = null;

        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosedElement;

                // Private constructors cannot be used.
                // Protected and package-private ones are ok because the serializer is generated in the same package as
                // the serialized type.
                if (constructorElement.getModifiers().contains(Modifier.PRIVATE)) {
                    continue;
                }

                List<Param> paramTypes = constructorElement.getParameters().stream()
                        .map(param -> new Param(
                                param.asType(),
                                param.getSimpleName().toString(),
                                typeMapper.findDynamoDBType(param.asType())
                        ))
                        .toList();

                Constructor current = new Constructor(paramTypes);

                // Prefer no-args constructor
                if (current.isNoArgs()) {
                    return current;
                }

                // Otherwise choose constructor with most parameters
                if (chosen == null || current.args().size() > chosen.args().size()) {
                    chosen = current;
                }
            }
        }

        return chosen;
    }

    private Map<String, BiFunction<TypeMirror, String, String>> buildCustomDeserializers() {
        Map<String, BiFunction<TypeMirror, String, String>> map = new HashMap<>();

        map.put("java.time.Instant", (instanceType, getter) -> "java.time.Instant.ofEpochMilli(Long.valueOf(" + getter + "))");

        return map;
    }

    private String customDeserializer(TypeMirror type, String getter) {
        BiFunction<TypeMirror, String, String> serializer = customDeserializers.getOrDefault(type.toString(),
                (aType, aGetter) ->
                        nameUtils.camelCase(nameUtils.serializerClassName(aType)) + ".deserialize(" + aGetter + ")"
        );
        return serializer.apply(type, getter);
    }

    private record Constructor(List<Param> args) {
        boolean isNoArgs() {
            return args.isEmpty();
        }
    }

    private record Param(TypeMirror type, String name, AttributeValue.Type ddbType) {
    }
}
