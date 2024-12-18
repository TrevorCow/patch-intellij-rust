package com.trevorcow.pir;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PatchVisitor extends ClassVisitor {

    protected PatchVisitor(ClassVisitor cv) {
        super(Opcodes.ASM8, cv);
    }

    /// See [...](https://github.com/intellij-rust/intellij-rust/blob/master/src/main/kotlin/org/rust/cargo/runconfig/RsExecutableRunner.kt#L55) for what we are patching
    /// Basically inserting `toolchainError = null` after L55 making L56 always fail
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        final String execute_sig = "(Lcom/intellij/execution/runners/ExecutionEnvironment;)V";

        if (name.equals("execute") && descriptor.equals(execute_sig)) {

            return new MethodVisitor(Opcodes.ASM8, super.visitMethod(access, name, descriptor, signature, exceptions)) {

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    // Looking for the call to `checkToolchainSupported`
                    if (
                            opcode == Opcodes.INVOKEVIRTUAL &&
                                    owner.equals("org/rust/cargo/runconfig/RsExecutableRunner") &&
                                    name.equals("checkToolchainSupported") &&
                                    descriptor.equals("(Lcom/intellij/openapi/project/Project;Lorg/rust/cargo/runconfig/RsToolchainHost;)Lorg/rust/cargo/runconfig/BuildResult$ToolchainError;") &&
                                    !isInterface
                    ) {
                        // When we find it, POP and discard the value, then push a null
                        // This causes the next check to `(toolchainError != null)`, which stops the run, to always fail continuing execution
                        super.visitInsn(Opcodes.POP);
                        super.visitInsn(Opcodes.ACONST_NULL);
                    }
                }
            };
        }


        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }


}
