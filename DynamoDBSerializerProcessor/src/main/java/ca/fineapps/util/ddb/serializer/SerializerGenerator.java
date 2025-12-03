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

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

class SerializerGenerator {
    private final Types typeUtils;
    private final Elements elementUtils;
    private final NameUtils nameUtils;

    public SerializerGenerator(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
        this.nameUtils = new NameUtils(typeUtils);
    }

    public Collection<TypeMirror> generateSerializer(TypeMirror type, Writer writer) throws IOException {
        Collection<TypeMirror> dependencies = new HashSet<>();

        generatePackageLine(type, writer);
        generateImports(writer, type);
        generateGeneratedLine(writer);
        generateClassNameLine(type, writer);

        generateSerializeMethod(type, writer, dependencies);
        generateDeserializeMethod(type, writer, dependencies);

        generateFields(dependencies, writer);
        generateConstructor(type, writer, dependencies);
        generateCreateMethod(type, writer, dependencies);

        // Close class
        writer.write("}\n");

        return dependencies;
    }

    private void generatePackageLine(TypeMirror type, Writer writer) throws IOException {
        TypeElement element = (TypeElement) typeUtils.asElement(type);
        writer.write("package " + elementUtils.getPackageOf(element).getQualifiedName().toString() + ";\n\n");
    }

    public void generateImports(Writer writer, TypeMirror type) throws IOException {
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

    private void generateGeneratedLine(Writer writer) throws IOException {
        String generatorName = getClass().getCanonicalName();
        String date = Instant.now().toString();

        writer.write(String.format("@Generated(value = \"%s\", date = \"%s\")\n", generatorName, date));
    }

    private void generateClassNameLine(TypeMirror type, Writer writer) throws IOException {
        writer.write("public class " + nameUtils.serializerClassName(type) +
                " implements Serializer<" + typeUtils.asElement(type).getSimpleName() + "> {\n\n");
    }

    private void generateSerializeMethod(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        writer.write("\t@Override\n");
        writer.write("\tpublic Map<String, AttributeValue> serialize(" + element.getSimpleName() + " object) {\n");
        writer.write("\t\tMap<String, AttributeValue> map = new HashMap<>();\n");
        writer.write("\n");

        FieldSerializer serializer = new FieldSerializer(typeUtils, elementUtils, nameUtils);
        serializer.generateFieldSerialization(type, writer, dependencies);

        writer.write("\n");
        writer.write("\t\treturn map;\n");
        writer.write("\t}\n");
        writer.write("\n");
    }

    private void generateDeserializeMethod(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        TypeElement element = (TypeElement) typeUtils.asElement(type);

        writer.write("\t@Override\n");
        writer.write("\tpublic " + element.getSimpleName() + " deserialize(Map<String, AttributeValue> map) {\n");

        FieldDeserializer deserializer = new FieldDeserializer(typeUtils, elementUtils, nameUtils);
        deserializer.generateFieldDeserialization(type, writer, dependencies);

        writer.write("\t}\n");
        writer.write("\n");
    }

    private void generateFields(Collection<TypeMirror> dependencies, Writer writer) throws IOException {
        for (TypeMirror dependency : dependencies) {
            String serializerName = nameUtils.serializerClassName(dependency);
            writer.write("\tprivate " + serializerName + " " + nameUtils.camelCase(serializerName) + ";\n");
        }

        if (!dependencies.isEmpty()) {
            writer.write("\n");
        }
    }

    private void generateConstructor(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        writer.write("\tprotected " + nameUtils.serializerClassName(type) + "(");
        writer.write(dependencies.stream()
                .map(nameUtils::serializerClassName)
                .map(className -> "\n\t\t\t" + className + " " + nameUtils.camelCase(className))
                .collect(Collectors.joining(","))
        );
        writer.write(") {\n");
        writer.write(dependencies.stream()
                .map(nameUtils::serializerClassName)
                .map(nameUtils::camelCase)
                .map(fieldName -> "\t\tthis." + fieldName + " = " + fieldName + ";\n")
                .collect(Collectors.joining(""))
        );
        writer.write("\t}\n\n");
    }

    private void generateCreateMethod(TypeMirror type, Writer writer, Collection<TypeMirror> dependencies) throws IOException {
        writer.write("\tpublic static " + nameUtils.serializerClassName(type) + " create() {\n");
        writer.write("\t\treturn new " + nameUtils.serializerClassName(type) + "(");
        writer.write(dependencies.stream()
                .map(nameUtils::serializerClassName)
                .map(className -> "\n\t\t\t\t" + className + ".create()")
                .collect(Collectors.joining(","))
        );
        if (!dependencies.isEmpty()) {
            writer.write("\n\t\t");
        }
        writer.write(");\n");
        writer.write("\t}\n");
    }
}
