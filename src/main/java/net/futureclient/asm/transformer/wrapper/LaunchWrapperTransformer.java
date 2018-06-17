package net.futureclient.asm.transformer.wrapper;

import net.futureclient.asm.AsmLib;
import net.futureclient.asm.config.Config;
import net.futureclient.asm.transformer.ClassTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public final class LaunchWrapperTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("asmlib");

    public LaunchWrapperTransformer() {}


    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
         List<ClassTransformer> classTransformers = this.getTransformers(transformedName);

        if (!classTransformers.isEmpty()) {
            ClassNode cn = new ClassNode();
            ClassReader cr = new ClassReader(basicClass);
            cr.accept(cn, 0);

            classTransformers.forEach(transformer -> {
                try {
                    transformer.transform(cn);
                    LOGGER.info("Successfully transformed class {}", transformer.getClassName()); // TODO; success message for each method
                } catch (Throwable throwable) {
                    LOGGER.log(Level.ERROR, "Error transforming \"{}\" with transformer \"{}\".", transformedName, transformer.getClass().getName());

                    if (transformer.isRequired())
                        throw throwable; // crash game
                    else
                        throwable.printStackTrace();
                }
            });

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            //log("MainOutput.class", cw.toByteArray());
            return cw.toByteArray();
        }

        return basicClass;
    }

    private void log(String fileName, byte[] bytes) {
        File f = new File(fileName);
        try {
            f.createNewFile();
            Files.write(f.toPath(), bytes);
        } catch (Exception e) {

        }
    }

    private List<ClassTransformer> getTransformers(String name) {
        return AsmLib.getConfigManager()
                .getConfigs().stream()
                .map(Config::getClassTransformers)
                .flatMap(List::stream)
                .filter(classTransformer -> classTransformer.getClassName().equals(name))
                .sorted(ClassTransformer::compareTo)
                .collect(Collectors.toList());
    }

}
