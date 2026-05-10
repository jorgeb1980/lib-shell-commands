package shell;

import jodd.io.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class TestShellCommandLauncher {

    @DisabledOnOs({OS.WINDOWS})
    @Test
    public void testUnixLikeLaunchCommand() {
        var launcher = ShellCommandLauncher.builder()
            .command("ls")
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
    public void testBlanksInArgumentsUnixLike() {
        testBlanksInArguments("ls");
    }

    @EnabledOnOs({OS.WINDOWS})
    @Test
    public void testBlanksInArgumentsWindows() {
        testBlanksInArguments("dir");
    }

    private void testBlanksInArguments(String command) {
        File testBlanksTmpDir = null;
        final var CHILD_DIRECTORY_WITH_BLANKS = "child directory with blanks";
        final var SOME_FILE = "some file";
        try {
            testBlanksTmpDir = Files.createTempDirectory("test_blanks").toFile();
            testBlanksTmpDir.deleteOnExit();

            // This will create something like '/tmp/test_blanks_whatever1234/child directory with blanks/some file'
            //  or something similar inside c:\Windows\Temp, c:\Users\whatever\AppData\Local\Temp or wherever it sees fit
            var childDirWithBlanks = new File(testBlanksTmpDir, CHILD_DIRECTORY_WITH_BLANKS);
            childDirWithBlanks.mkdir();
            var someFile = new File(childDirWithBlanks, SOME_FILE);
            someFile.createNewFile();

            var results = ShellCommandLauncher.builder()
                .command(command)
                .parameter(CHILD_DIRECTORY_WITH_BLANKS)
                .cwd(testBlanksTmpDir)
                .build().launch();

            assertEquals(0, results.getExitCode());
            assertTrue(results.getStandardOutput().contains(SOME_FILE));

        } catch (IOException | ShellException e) {
            fail(e);
        } finally {
            if (testBlanksTmpDir != null && testBlanksTmpDir.exists() && testBlanksTmpDir.isDirectory()) {
                try { FileUtil.deleteDir(testBlanksTmpDir); } catch (IOException ioe) { fail(ioe); }
            }
        }
    }

    @DisabledOnOs({OS.WINDOWS})
    @Test
    public void testUnixLikeErrorConditions() {
        assertThrows(ShellException.class, () -> {
            ShellCommandLauncher.builder()
                .command("ls")
                .cwd(new File("/this/directory/should/not/exist"))
                .parameter("-lah")
                .build().launch();
        });
        assertThrows(ShellException.class, () -> {
            ShellCommandLauncher.builder()
                .command("ls")
                // Should exist, but not a directory
                .cwd(new File("/bin/sh"))
                .parameter("-lah")
                .build().launch();
        });
    }


    @EnabledOnOs({OS.WINDOWS})
    @Test
    public void testWindowsLaunchCommand() {
        var launcher = ShellCommandLauncher.builder()
            .command("dir")
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
            ShellCommandLauncher.builder()
                .command("dir")
                .parameter("/A")
                .cwd(new File("c:\\this\\directory\\should\\not\\exist"))
                .build().launch();
        });
        assertThrows(ShellException.class, () -> {
            ShellCommandLauncher.builder()
                .command("dir")
                .parameter("/A")
                // Should exist, but not a directory
                .cwd(new File("c:\\windows\\explorer.exe"))
                .build().launch();
        });
    }

    @Test
    public void testNonNull() {
        assertThrows(NullPointerException.class, () -> {
            ShellCommandLauncher.builder()
                // missing command parameter
                .parameter("dir")
                .cwd(new File(System.getProperty("java.io.tmpdir")))
                .build();
        });
    }

    private void testEnvVariables(String commandName) {
        final var SOME_VARIABLE = "SOME_VARIABLE";
        final var SOME_OTHER_VARIABLE = "SOME_OTHER_VARIABLE";
        final var VALUE_1 = "abcd1";
        final var VALUE_2 = "a b c d 2";
        var launcher = ShellCommandLauncher.builder()
            .command(commandName)
            .env(SOME_VARIABLE, VALUE_1)
            .env(SOME_OTHER_VARIABLE, VALUE_2)
            .build();
        try {
            var results = launcher.launch();
            var variables = new EnvVariablesParser().parseAsMap(results.getStandardOutput());
            assertTrue(variables.containsKey(SOME_VARIABLE));
            assertEquals(VALUE_1, variables.get(SOME_VARIABLE));
            assertTrue(variables.containsKey(SOME_OTHER_VARIABLE));
            assertEquals(VALUE_2, variables.get(SOME_OTHER_VARIABLE));
        } catch(ShellException e) {
            fail(e);
        }
    }
    
    @EnabledOnOs({OS.WINDOWS})
    @Test
    public void testWindowsEnvVars() {
        testEnvVariables("set");
    }

    @DisabledOnOs({OS.WINDOWS})
    @Test
    public void testUnixLikeEnvVars() {
        testEnvVariables("env");
    }

    @EnabledOnOs({OS.WINDOWS})
    @Test
    public void redirectStdinWindows() {
        var someText =
                """
                lalala
                lolololo
                lerelelele
                """;

        try {
            var result = ShellCommandLauncher.builder().
                    command("find").
                    parameter("/c").parameter("/v").parameter("\"\"").
                    stdin(someText.getBytes(StandardCharsets.ISO_8859_1)).build().launch();
            assertEquals(0, result.getExitCode());
            assertEquals("3", result.getStandardOutput().trim());
        } catch (ShellException e) {
            fail(e);
        }
    }

    @DisabledOnOs({OS.WINDOWS})
    @Test
    public void redirectStdinUnixLike() {
        var someText =
                """
                some
                text
                """;

        try {
            var result = ShellCommandLauncher.builder().
                    command("wc").
                    parameter("-l").
                    stdin(someText.getBytes(StandardCharsets.UTF_8)).build().launch();
            assertEquals(0, result.getExitCode());
            assertEquals("2", result.getStandardOutput().trim());
        } catch (ShellException e) {
            fail(e);
        }
    }
}
