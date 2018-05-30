package net.futureclient.asm.transformer.util;

import net.futureclient.asm.AsmLib;
import net.futureclient.asm.transformer.ClassTransformer;
import net.futureclient.asm.transformer.MethodTransformer;
import net.futureclient.asm.transformer.annotation.Inject;
import net.futureclient.asm.transformer.annotation.Transformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by Babbaj on 5/17/2018.
 */
// TODO: make this not bad
public final class TransformerGenerator {
    private TransformerGenerator() {}

    // group 1 = method name;
    // group 2 = args/return type
    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile("(.+)(\\(.+\\).+)");

    private static final Collection<Class<? extends Annotation>> METHOD_ANNOTATION_CLASSES = Arrays.asList(
            Inject.class
    );

    //@Deprecated
    public static ClassTransformer fromClass(Class<?> clazz) {
        checkClass(clazz);

        AnnotationInfo info = AsmLib.transformerAnnotations.get(clazz.getName());
        try {
            Object instance = clazz.newInstance();
            String targetClass = info.<List<Type>>getValue("value").get(0).getClassName(); // TODO: check both targets and value
            ClassTransformer transformer = new ClassTransformer(targetClass) {};//new ReflectiveClassTransformer(instance, info.targets()[0]);
            Stream.of(clazz.getDeclaredMethods())
                    .filter(m -> !isConstructor(m))
                    .filter(m -> isValidMethodTransformer(m))
                    .map(m -> createMethodTransformer(m, instance))
                    .forEach(transformer.getMethodTransformers()::add);
            return transformer;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // checks if a class is a valid transformer class
    private static void checkClass(Class<?> clazz) {
        // TODO: add annotation or something to show that a class has been processed by the preprocessor
        //if (!clazz.isAnnotationPresent(Transformer.class))
        //  throw new IllegalArgumentException("Missing @Transformer annotation");
        if (!hasValidConstructor(clazz))
            throw new IllegalArgumentException("Invalid constructor, expected 0 args");
    }

    // create a method transformer that encapsulates a java.lang.reflect.Method
    // TODO: support more annotations
    @Deprecated
    public static MethodTransformer createMethodTransformer(Method method, Object instance) {
        Inject info = method.getAnnotation(Inject.class);
        final String[] parsed = parseTarget(info.target());
        String name = parsed[0];
        String desc = parsed[1];

        final Class<?>[] params = method.getParameterTypes(); // TODO: stuff

        return new MethodTransformer(name, desc) { // maybe don't use anonymous class?
            @Override
            public void inject(MethodNode methodNode) {
                try {
                    method.invoke(instance, methodNode);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        };
    }

    private static String[] parseTarget(String method) {
        final Matcher m = DESCRIPTOR_PATTERN.matcher(method);
        if (!m.matches()) throw new IllegalArgumentException("Invalid method descriptor");
        return new String[] {m.group(1), m.group(2)};
    }

    /**
     * A method represents a MethodTransformer if and only if it is annotated with a single meme annotation such as @Inject
     */
    private static boolean isValidMethodTransformer(Method m) {
        return Stream.of(m.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .filter(METHOD_ANNOTATION_CLASSES::contains)
                .count() == 1;
    }

    private static boolean hasValidConstructor(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean isConstructor(Method m) {
        return m.getName().equals("<init>") || m.getName().equals("<clinit>");
    }
}
