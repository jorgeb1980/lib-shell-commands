package shell;


import lombok.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import static shell.OSDetection.isWindows;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShellCommandLauncher {

    @NonNull
    private String command;
    @Singular
    private List<String> parameters;
    @Singular
    private Map<String, String> envs;
    private byte[] stdin;
    private File cwd;

    public ExecutionResults launch() throws ShellException {
        var builder = CommandLauncher.builder();
        if (isWindows()) {
            builder.program("cmd.exe")
                .parameter("/c")
                .parameter(command);
        } else {
            builder.program("/usr/bin/env")
                .parameter("--")
                .parameter(command);
        }
        builder.stdin(stdin);
        parameters.forEach(builder::parameter);
        builder.envs(envs).cwd(cwd);
        return builder.build().launch();
    }
}
