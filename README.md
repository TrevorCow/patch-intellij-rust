# JetBrains Rust Plugin Patcher

This Java program provides a temporary fix for the JetBrains Rust plugin issue [RUST-13396](https://youtrack.jetbrains.com/issue/RUST-13396). It allows users to patch the plugin jar file to address the issue until an official update is available.

## Usage
Run the program with one of the following options:

```bash
java -jar rust-plugin-patcher.jar [options]
```

### Options
- `-i`: Enables interactive mode to list installed JetBrains products and select a plugin jar file.
- `[path-to-plugin-jar]`: Provide the path to the Rust plugin jar file to be patched.
- `-o [output-path]`: Specify the output path or filename for the patched jar file. If not provided, the patched file will be saved in the same directory as the original.

### Examples
#### Interactive Mode
```bash
java -jar rust-plugin-patcher.jar -i
```
This will list all installed JetBrains products and guide you to select the Rust plugin jar file.

#### Direct Path with Default Output
```bash
java -jar rust-plugin-patcher.jar /path/to/rust-plugin.jar
```
This patches the specified plugin jar file and saves the patched file in the same directory as the original.

#### Direct Path with Custom Output
```bash
java -jar rust-plugin-patcher.jar /path/to/rust-plugin.jar -o /path/to/patched-plugin.jar
```
This patches the specified plugin jar file and saves the output to the specified path or filename.
