package shell;

import jodd.io.StreamGobbler;
import lombok.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static shell.OSDetection.isWindows;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandLauncher {

    @NonNull
    private String program;
    @Singular
    private List<String> parameters;
    @Singular
    private Map<String, String> envs;
    private byte[] stdin;
    private File cwd;

    private String buildString(byte[] content) {
        return new String(content, isWindows() ? Charset.forName("cp1252") : StandardCharsets.UTF_8);
    }

    public ExecutionResults launch() throws ShellException {
        try {
            List<String> command = new LinkedList<>();
            command.add(program);
            command.addAll(parameters);
            var pb = new ProcessBuilder(command);
            if (cwd == null) cwd = new File(System.getProperty("user.dir"));
            else {
                // Some sanity check on cwd
                if (!cwd.exists()) {
                    throw new ShellException(format("Directory %s does not exist", cwd));
                } else if (!cwd.isDirectory()) {
                    throw new ShellException(format("%s is not a directory", cwd));
                }
            }
            if (stdin != null) { pb.redirectInput(ProcessBuilder.Redirect.PIPE); }
            pb.directory(cwd);
            if (envs != null && !envs.isEmpty()) {
                var environment = pb.environment();
                environment.putAll(envs);
            }
            var standardOutputStream = new ByteArrayOutputStream();
            var errorOutputStream = new ByteArrayOutputStream();
            var p = pb.start();
            var outputGobbler = new StreamGobbler(p.getInputStream(), standardOutputStream);
            outputGobbler.start();
            var errorGobbler = new StreamGobbler(p.getErrorStream(), errorOutputStream);
            errorGobbler.start();
            if (stdin != null) {
                p.getOutputStream().write(stdin);
                try {
                    p.getOutputStream().close();
                } catch (IOException e) {
                    throw new ShellException(e);
                }
            }

            var exitCode = p.waitFor();
            outputGobbler.waitFor();
            errorGobbler.waitFor();
            return ExecutionResults.builder().
                exitCode(exitCode).
                errorOutput(buildString(errorOutputStream.toByteArray())).
                standardOutput(buildString(standardOutputStream.toByteArray())).
                build();
        } catch (IOException | InterruptedException e) {
            throw new ShellException(e);
        }
    }
}
