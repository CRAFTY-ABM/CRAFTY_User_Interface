package de.cesr.crafty.utils.analysis;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PackageFinderWithClasses {
    public static void main(String[] args) throws IOException {
        // Adjust the source path if needed
        Path root = Paths.get("src/main/java");

        // A map from package name to list of class names
        // Using TreeMap to keep packages in alphabetical order
        Map<String, List<String>> packageClassMap = new TreeMap<>();

        // Regex to match class/interface/enum declarations
        // Captures the type (class/interface/enum) and the name in groups
        // Example match: "public class MyClass" or "abstract class HelloWorld"
        Pattern classPattern = Pattern.compile(
            "\\b(?:public\\s+|protected\\s+|private\\s+|static\\s+|abstract\\s+|final\\s+)*" +
            "(class|interface|enum)\\s+(\\w+)"
        );

        // Walk the file tree, looking for .java files
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> {
                     // Default to the no-package (unnamed) if none is found
                     String currentPackage = "";
                     List<String> classesInThisFile = new ArrayList<>();

                     try {
                         List<String> lines = Files.readAllLines(path);
                         for (String line : lines) {
                             line = line.trim();

                             // Check for package statement
                             if (line.startsWith("package ")) {
                                 // Extract the package name
                                 // e.g. "package com.example.foo;"
                                 currentPackage = line
                                     .replaceFirst("package\\s+", "")
                                     .replace(";", "")
                                     .trim();
                             }

                             // Find any class/interface/enum matches
                             Matcher matcher = classPattern.matcher(line);
                             while (matcher.find()) {
                                 String type = matcher.group(1);
                                 String className = matcher.group(2);
                                 // Optionally combine type and name, e.g. "class MyClass"
                                 // For now, we'll store just the class name.
                                 classesInThisFile.add(className);
                             }
                         }

                         // If no package statement, we treat it as the default package
                         if (currentPackage.isEmpty()) {
                             currentPackage = "(default)";
                         }

                         // Store the found classes into the map
                         if (!classesInThisFile.isEmpty()) {
                             // Ensure the map has an entry
                             packageClassMap.putIfAbsent(currentPackage, new ArrayList<>());
                             // Add all newly discovered classes
                             packageClassMap.get(currentPackage).addAll(classesInThisFile);
                         }

                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 });
        }

        // Sort the class names within each package
        for (Map.Entry<String, List<String>> entry : packageClassMap.entrySet()) {
            Collections.sort(entry.getValue());
        }

        // Print out the results
        packageClassMap.forEach((pkg, classes) -> {
            System.out.println("Package: " + pkg);
            for (String className : classes) {
                System.out.println("  - " + className);
            }
        });
    }
}
