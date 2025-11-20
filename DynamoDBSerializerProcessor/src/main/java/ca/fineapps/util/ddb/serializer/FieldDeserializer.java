package ca.fineapps.util.ddb.serializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

class FieldDeserializer {
    private final Types typeUtils;
    private final DynamoDBTypeMapper typeMapper;

    public FieldDeserializer(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.typeMapper = new DynamoDBTypeMapper(typeUtils, elementUtils);
    }

    void generateFieldDeserialization(StringWriter writer, TypeMirror type) {
        Element element = typeUtils.asElement(type);
        Constructor constructor = findConstructor(element);

        if (constructor != null) {
            if (constructor.isNoArgs()) {
                writer.write("\t\t" + element.getSimpleName() + " object = new " + element.getSimpleName() + "();\n");
                writer.write("\n");
                generateFieldDeserializationWithSetters(writer, type);
                writer.write("\t\treturn object;\n");
            } else {
                writer.write("\t\treturn new " + element.getSimpleName() + "(\n");
                generateFieldDeserializationWithConstructorParameters(writer, constructor);
                writer.write("\t\t);\n");
            }
        }
    }

    private void generateFieldDeserializationWithSetters(StringWriter writer, TypeMirror type) {
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
                    AttributeValue.Type ddbType = typeMapper.findDynamoDBType(parameters.getFirst().asType());

                    String fieldName = Character.toLowerCase(enclosedElementName.charAt(3)) +
                            (enclosedElementName.length() > 4 ? enclosedElementName.substring(4) : "");
                    writer.write("\t\tif (map.containsKey(\"" + fieldName + "\")) {\n");

                    String mapGetter = "map.get(\"" + fieldName + "\")." + ddbType.name().toLowerCase() + "()";
                    mapGetter = wrapMapGetter(parameters.getFirst().asType(), mapGetter);

                    writer.write("\t\t\tobject." + enclosedElement.getSimpleName() + "(" +
                            mapGetter +
                            ");\n");
                    writer.write("\t\t}\n");
                    writer.write("\n");
                }
            }
        }
    }

    private void generateFieldDeserializationWithConstructorParameters(StringWriter writer, Constructor constructor) {
        writer.write(constructor.args().stream()
                .map(this::mapConstructorArg)
                .collect(Collectors.joining(",\n")) + "\n"
        );
    }

    private String mapConstructorArg(Param arg) {
        return "\t\t\t\tmap.containsKey(\"" + arg.name() + "\") ? " +
                wrapMapGetter(arg.type(),
                        "map.get(\"" + arg.name() + "\")." +
                                typeMapper.findDynamoDBType(arg.type()).name().toLowerCase() + "()"
                ) + " : " +
                (arg.type().getKind().isPrimitive() ? "0" : "null");
    }

    private String wrapMapGetter(TypeMirror type, String mapGetter) {
        String template = switch (type.toString()) {
            case "int", "java.lang.Integer" -> "Integer.parseInt(%s)";
            case "long", "java.lang.Long" -> "Long.parseLong(%s)";
            case "double", "java.lang.Double" -> "Double.parseDouble(%s)";
            case "boolean", "java.lang.Boolean" -> "Boolean.parseBoolean(%s)";
            case "short", "java.lang.Short" -> "Short.parseShort(%s)";
            case "byte", "java.lang.Byte" -> "Byte.parseByte(%s)";
            case "float", "java.lang.Float" -> "Float.parseFloat(%s)";
            default -> null;
        };

        return template == null ? mapGetter : String.format(template, mapGetter);
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
                        .map(param -> new Param(param.asType(), param.getSimpleName().toString()))
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

    private record Constructor(List<Param> args) {
        boolean isNoArgs() {
            return args.isEmpty();
        }
    }

    private record Param(TypeMirror type, String name) {
    }
}
