package shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class TestCommandLauncher {

    @DisabledOnOs({OS.WINDOWS})
    @Test
    public void testUnixLikeLaunchCommand() {
        var launcher = CommandLauncher.builder()
            .program("/usr/bin/env")
            .parameter("--")
            .parameter("ls")
            .cwd(new File(System.getProperty("java.io.tmpdir")))
            .parameter("-lah")
            .build();
        try {
            var results = launcher.launch();
            assertEquals(0, results.getExitCode());
            assertNotNull(results.getStandardOutput());
        } catch(ShellException e) {
            fail(e);
        }
    }

    @DisabledOnOs({OS.WINDOWS})
    @Test
    public void testUnixLikeErrorConditions() {
        assertThrows(ShellException.class, () -> {
            CommandLauncher.builder()
                .program("/usr/bin/env")
                .parameter("--")
                .parameter("ls")
                .cwd(new File("/this/directory/should/not/exist"))
                .parameter("-lah")
                .build().launch();
        });
        assertThrows(ShellException.class, () -> {
            CommandLauncher.builder()
                .program("/usr/bin/env")
                .parameter("--")
                .parameter("ls")
                // Should exist, but not a directory
                .cwd(new File("/bin/sh"))
                .parameter("-lah")
                .build().launch();
        });
    }


    @EnabledOnOs({OS.WINDOWS})
    @Test
    public void testWindowsLaunchCommand() {
        var launcher = CommandLauncher.builder()
            .program("cmd.exe")
            .parameter("/c")
            .parameter("dir")
            .parameter("/A")
            .cwd(new File(System.getProperty("java.io.tmpdir")))
            .build();
        try {
            var results = launcher.launch();
            assertEquals(0, results.getExitCode());
            assertNotNull(results.getStandardOutput());
        } catch(ShellException e) {
            fail(e);
        }
    }

    @EnabledOnOs({OS.WINDOWS})
    @Test
    public void testWindowsErrorConditions() {
        assertThrows(ShellException.class, () -> {
            CommandLauncher.builder()
                .program("cmd.exe")
                .parameter("/c")
                .parameter("dir")
                .parameter("/A")
                .cwd(new File("c:\\this\\directory\\should\\not\\exist"))
                .build().launch();
        });
        assertThrows(ShellException.class, () -> {
            CommandLauncher.builder()
                .program("cmd.exe")
                .parameter("/c")
                .parameter("dir")
                .parameter("/A")
                // Should exist, but not a directory
                .cwd(new File("c:\\windows\\explorer.exe"))
                .build().launch();
        });
    }

    @Test
    public void testNonNull() {
        assertThrows(NullPointerException.class, () -> {
            CommandLauncher.builder()
                // missing program
                .parameter("cmd.exe")
                .parameter("/c")
                .parameter("dir")
                .cwd(new File(System.getProperty("java.io.tmpdir")))
                .build();
        });
    }

}
