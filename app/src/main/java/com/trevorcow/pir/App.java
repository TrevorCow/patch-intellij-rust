package com.trevorcow.pir;

import org.apache.commons.cli.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

public class App {

    public static byte[] patch_RsExecutableRunnerkt(InputStream clazzInputStream) throws IOException {
        ClassReader classReader = new ClassReader(clazzInputStream);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        PatchVisitor patchVisitor = new PatchVisitor(classWriter);
        classReader.accept(patchVisitor, 0);

        return classWriter.toByteArray();
    }

    static void create_patched_jar(File inputJarFile, File outputJarFile) throws IOException {

        try (JarFile inputJar = new JarFile(inputJarFile)) {
            {
                JarEntry entry = inputJar.getJarEntry("org/rust/cargo/runconfig/RsExecutableRunner" + ".class");
                if (entry == null) {
                    System.err.println("Can't find RsExecutableRunner.class");
                    return;
                }
            }


            try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJarFile))) {
                for (Iterator<JarEntry> it = inputJar.entries().asIterator(); it.hasNext(); ) {
                    JarEntry entry = it.next();

                    try (InputStream is = inputJar.getInputStream(entry)) {
                        jarOutputStream.putNextEntry(entry);

                        if (entry.getName().equals("org/rust/cargo/runconfig/RsExecutableRunner" + ".class")) {
                            byte[] patched_class = patch_RsExecutableRunnerkt(is);
                            jarOutputStream.write(patched_class);
                        } else {
                            jarOutputStream.write(is.readAllBytes());
                        }

                        jarOutputStream.closeEntry();
                    }
                }


            }


        }

    }

    public static ArrayList<File> find_installed_jbproducts() {
        final String[] ignore = {"consentOptions"};

        final Path default_windows_path = Path.of(System.getenv("APPDATA")).resolve("JetBrains");
        final File jb_folder = default_windows_path.toFile();
        final File[] files = jb_folder.listFiles();
        if (files == null) {
            System.err.println("Can't find JetBrains folder");
            return null;
        }

        ArrayList<File> result = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory() && Stream.of(ignore).noneMatch(s -> s.equalsIgnoreCase(file.getName()))) {
                result.add(file);
            }
        }
        return result;
    }

    public static int _main(String[] args) throws IOException {
        // Create Options object
        Options options = new Options();

        Option outputPathOption = Option.builder("o")
                .argName("outputPath")
                .hasArg()
                .numberOfArgs(1).type(Path.class)
                .build();

        options.addOption(outputPathOption);

        Option interactive_option = Option.builder("i")
                .argName("interactive")
                .hasArg(false)
                .build();
        options.addOption(interactive_option);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            boolean interactive = cmd.hasOption(interactive_option.getOpt());

            File inputJarFile = null;
            if (interactive) {
                var files = find_installed_jbproducts();
                if (files == null) {
                    System.err.println("Can't find JetBrains folder");
                    return 1;
                }
                System.out.println(files);

                // Get user input
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    System.out.printf("%2d) %s\n", i, file.getName());
                }
                // Don't close scanner because we don't own System.in
                Scanner scanner = new Scanner(System.in);
                int choice = scanner.nextInt();
                File directory_choice = files.get(choice);
                File plugin_jar_directory = directory_choice.toPath().resolve("plugins/intellij-rust/lib").toFile();
                File[] plugin_jar_directory_files = plugin_jar_directory.listFiles();
                if (plugin_jar_directory_files == null) {
                    System.err.println("Can't find plugin jar directory (Is the rust plugin installed?)");
                    return 1;
                }
                for (File file : plugin_jar_directory_files) {
                    if (file.getName().startsWith("intellij-rust")) {
                        inputJarFile = file;
                    }
                }

                if (inputJarFile == null) {
                    System.err.println("Can't find jar starting with `intellij-rust` (Might have been renamed)");
                    return 1;
                }
            } else {
                inputJarFile = new File(cmd.getArgs()[0]);
            }


            File outputJarFile;
            if (cmd.hasOption("o")) {
                outputJarFile = new File(cmd.getOptionValue("o"));
            } else {
                Path outputPathParent = inputJarFile.toPath().getParent();

                int lastPeriod = inputJarFile.getName().lastIndexOf(".");
                String originalName = inputJarFile.getName().substring(0, lastPeriod);
                String originalExtension = inputJarFile.getName().substring(lastPeriod);
                String outputFileName = originalName + "-patched" + originalExtension;

                Path outputPath = outputPathParent.resolve(outputFileName);
                outputJarFile = outputPath.toFile();
            }

            if (interactive) {
                File renamed_file = inputJarFile.toPath().getParent().resolve(inputJarFile.getName() + ".orig").toFile();
                System.out.printf("Interactive mode enabled, renaming input jar file from `%s` to `%s`\n", inputJarFile.getName(), renamed_file);
                if (!inputJarFile.renameTo(renamed_file)){
                    System.err.println("Can't rename jar file!");
                    return 1;
                }
                inputJarFile = renamed_file;
            }

            System.out.println("inputJarFile=" + inputJarFile.getAbsolutePath());
            System.out.println("outputJarFile=" + outputJarFile.getAbsolutePath());

            create_patched_jar(inputJarFile, outputJarFile);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void main(String[] args) throws IOException {
        System.exit(_main(args));
    }
}