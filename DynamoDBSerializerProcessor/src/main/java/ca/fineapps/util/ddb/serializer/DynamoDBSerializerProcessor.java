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
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes("ca.fineapps.util.ddb.serializer.Serialize")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DynamoDBSerializerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<TypeMirror> classesToSerialize = new ArrayList<>();

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
                            classesToSerialize.add(element.asType());
                        }
                    } catch (MirroredTypeException ex) {
                        TypeMirror typeMirror = ex.getTypeMirror();
                        if (typeMirror.getKind() != TypeKind.VOID) {
                            processingEnv.getMessager().printError(
                                    "When @Serializer is used on a class type, it generates a serializer " +
                                            "for the annotated type. A target type cannot be specified");
                        } else {
                            classesToSerialize.add(element.asType());
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
                            classesToSerialize.add(typeMirror);
                        }
                    }
                } else {
                    processingEnv.getMessager().printError("@Serializer can only be " +
                            "used on a class type or an interface type with a target type specified");
                }
            }
        }

        SerializerGenerator generator = new SerializerGenerator(
                processingEnv.getTypeUtils(),
                processingEnv.getElementUtils()
        );
        for (TypeMirror typeMirror : classesToSerialize) {
            try {
                JavaFileObject generatedSourceFile = generateSourceFile(typeMirror);
                try (Writer writer = generatedSourceFile.openWriter()) {
                    writer.write(generator.generateSerializer(typeMirror));
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
                ((TypeElement) enclosing).getQualifiedName().toString() : null;
        String className = type.getSimpleName().toString();

        String fullyQualifiedName = packageName + "." + (enclosingTypeName != null ? enclosingTypeName + "_" : "") +
                className;

        return processingEnv.getFiler().createSourceFile(fullyQualifiedName + "Serializer");
    }
}
