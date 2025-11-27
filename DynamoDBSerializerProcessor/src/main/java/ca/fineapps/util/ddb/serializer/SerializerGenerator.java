package ca.fineapps.util.ddb.serializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.StringWriter;
import java.time.Instant;

public class SerializerGenerator {
    private final Types typeUtils;
    private final Elements elementUtils;

    public SerializerGenerator(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
    }

    public String generateSerializer(TypeMirror type) {
        StringWriter writer = new StringWriter();

        generatePackageLine(writer, type);
        generateImports(writer, type);
        generateGeneratedLine(writer);
        generateClassNameLine(writer, type);

        generateSerializeMethod(writer, type);
        generateDeserializeMethod(writer, type);

        // Close class
        writer.write("}\n");

        return writer.toString();
    }

    private void generatePackageLine(StringWriter writer, TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);
        writer.write("package " + elementUtils.getPackageOf(element).getQualifiedName().toString() + ";\n\n");
    }

    public void generateImports(StringWriter writer, TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);
        String fullyQualifiedName = element.getQualifiedName().toString();

        writer.write("import " + fullyQualifiedName + ";\n");
        writer.write("import ca.fineapps.util.ddb.serializer.Serializer;\n");
        writer.write("import software.amazon.awssdk.core.SdkBytes;\n");
        writer.write("import software.amazon.awssdk.services.dynamodb.model.AttributeValue;\n\n");
        writer.write("import javax.annotation.processing.Generated;\n");
        writer.write("import java.util.Arrays;\n");
        writer.write("import java.util.HashMap;\n");
        writer.write("import java.util.Map;\n");
        writer.write("import java.util.stream.Stream;\n");

        writer.write("\n");
    }

    private void generateGeneratedLine(StringWriter writer) {
        String generatorName = getClass().getCanonicalName();
        String date = Instant.now().toString();

        writer.write(String.format("@Generated(value = \"%s\", date = \"%s\")\n", generatorName, date));
    }

    private void generateClassNameLine(StringWriter writer, TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        Element enclosing = element.getEnclosingElement();
        String enclosingTypeName = enclosing instanceof TypeElement ? enclosing.getSimpleName() + "_" : "";
        String className = enclosingTypeName + element.getSimpleName().toString() + "Serializer";

        writer.write("public class " + className + " implements Serializer<" + element.getSimpleName() + "> {\n\n");
    }

    private void generateSerializeMethod(StringWriter writer, TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        writer.write("\t@Override\n");
        writer.write("\tpublic Map<String, AttributeValue> serialize(" + element.getSimpleName() + " object) {\n");
        writer.write("\t\tMap<String, AttributeValue> map = new HashMap<>();\n");
        writer.write("\n");
        new FieldSerializer(typeUtils, elementUtils).generateFieldSerialization(writer, type);
        writer.write("\n");
        writer.write("\t\treturn map;\n");
        writer.write("\t}\n");
        writer.write("\n");
    }

    private void generateDeserializeMethod(StringWriter writer, TypeMirror type) {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        writer.write("\t@Override\n");
        writer.write("\tpublic " + element.getSimpleName() + " deserialize(Map<String, AttributeValue> map) {\n");
        new FieldDeserializer(typeUtils, elementUtils).generateFieldDeserialization(writer, type);
        writer.write("\t}\n");
        writer.write("\n");
    }
}
