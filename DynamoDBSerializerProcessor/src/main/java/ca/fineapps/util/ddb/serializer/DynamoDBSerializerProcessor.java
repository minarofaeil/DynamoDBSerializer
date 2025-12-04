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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes("ca.fineapps.util.ddb.serializer.Serialize")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DynamoDBSerializerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Types typeUtils = processingEnv.getTypeUtils();
        Set<EquatableTypeMirror> typesToSerialize = new HashSet<>();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.RECORD) {
                    try {
                        Class<?> value = element.getAnnotation(Serialize.class).value();
                        if (!Objects.equals(value, void.class)) {
                            processingEnv.getMessager().printError(
                                    "When @Serializer is used on a class type, it generates a serializer " +
                                            "for the annotated type. A target type cannot be specified");
                        } else {
                            typesToSerialize.add(new EquatableTypeMirror(typeUtils, element.asType()));
                        }
                    } catch (MirroredTypeException ex) {
                        TypeMirror typeMirror = ex.getTypeMirror();
                        if (typeMirror.getKind() != TypeKind.VOID) {
                            processingEnv.getMessager().printError(
                                    "When @Serializer is used on a class type, it generates a serializer " +
                                            "for the annotated type. A target type cannot be specified");
                        } else {
                            typesToSerialize.add(new EquatableTypeMirror(typeUtils, element.asType()));
                        }
                    }
                } else if (element.getKind() == ElementKind.INTERFACE) {
                    try {
                        Class<?> value = element.getAnnotation(Serialize.class).value();
                        if (Objects.equals(value, void.class)) {
                            processingEnv.getMessager().printError(
                                    "When @Serializer is used on an interface type, a target type must be specified");
                        } else {
                            processingEnv.getElementUtils().getTypeElement(value.getCanonicalName());
                        }
                    } catch (MirroredTypeException ex) {
                        TypeMirror typeMirror = ex.getTypeMirror();
                        if (typeMirror.getKind() == TypeKind.VOID) {
                            processingEnv.getMessager().printError(
                                    "When @Serializer is used on an interface type, a target type must be specified");
                        } else {
                            typesToSerialize.add(new EquatableTypeMirror(typeUtils, typeMirror));
                        }
                    }
                } else {
                    processingEnv.getMessager().printError("@Serializer can only be " +
                            "used on a class type or an interface type with a target type specified");
                }
            }
        }

        List<TypeMirror> typesToSerializeList = new ArrayList<>(
                typesToSerialize.stream().map(EquatableTypeMirror::getType).toList()
        );

        if (!typesToSerializeList.isEmpty()) {
            Map.of(
                    "ca.fineapps.util.ddb.serializer.ArrayCollector", "/source/java/ArrayCollector.java",
                    "ca.fineapps.util.ddb.serializer.Collectors", "/source/java/Collectors.java",
                    "ca.fineapps.util.ddb.serializer.Serializer", "/source/java/Serializer.java"
            ).forEach(this::copySourceFile);
        }

        for (int i = 0; i < typesToSerializeList.size(); i++) {
            TypeMirror typeMirror = typesToSerializeList.get(i);

            try {
                JavaFileObject generatedSourceFile = generateSourceFile(typeMirror);

                SerializerGenerator generator = new SerializerGenerator(
                        typeUtils,
                        processingEnv.getElementUtils()
                );

                try (Writer writer = generatedSourceFile.openWriter()) {
                    Collection<TypeMirror> dependencies = generator.generateSerializer(typeMirror, writer);

                    for (TypeMirror dependency : dependencies) {
                        EquatableTypeMirror equatableDependency = new EquatableTypeMirror(typeUtils, dependency);
                        if (!typesToSerialize.contains(equatableDependency)) {
                            typesToSerialize.add(equatableDependency);
                            typesToSerializeList.add(dependency);
                        }
                    }
                }
            } catch (IOException ex) {
                processingEnv.getMessager().printError("Failed to generate source file for " + typeMirror);
            }
        }

        return false;
    }

    private JavaFileObject generateSourceFile(TypeMirror typeMirror) throws IOException {
        Types typeUtils = processingEnv.getTypeUtils();
        TypeElement type = ((TypeElement) typeUtils.asElement(typeMirror));

        String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        Element enclosing = type.getEnclosingElement();
        String enclosingTypeName = enclosing instanceof TypeElement ?
                enclosing.getSimpleName().toString() : null;
        String className = type.getSimpleName().toString();

        String fullyQualifiedName = packageName + "." + (enclosingTypeName != null ? enclosingTypeName + "_" : "") +
                className;

        return processingEnv.getFiler().createSourceFile(fullyQualifiedName + "Serializer");
    }

    private void copySourceFile(String className, String resourcePath) {
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(className);
            byte[] buffer = new byte[5120];
            try (InputStream inputStream = getClass().getResourceAsStream(resourcePath);
                 Writer writer = sourceFile.openWriter()) {
                assert inputStream != null;

                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    writer.write(new String(buffer, 0, read));
                }
            }
        } catch (IOException ex) {
            processingEnv.getMessager().printError("Failed to generate source file for " + className);
        }
    }
}
