/*
 * Copyright © 2015 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.graphviz.engine;

import guru.nidi.graphviz.executor.ICommandExecutor;
import org.apache.commons.exec.CommandLine;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static guru.nidi.graphviz.engine.Format.SVG_STANDALONE;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EngineTest {
    private static final String START1_4 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + System.getProperty("line.separator") +
            "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"" + System.getProperty("line.separator") +
            " \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">" + System.getProperty("line.separator") +
            "<!-- Generated by graphviz version 2.38.0 (20140413.2041)" + System.getProperty("line.separator") +
            " -->" + System.getProperty("line.separator") +
            "<!-- Title: g Pages: 1 -->" + System.getProperty("line.separator") +
            "<svg";
    private static final String START1_7 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"\n" +
            " \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" +
            "<!-- Generated by graphviz version 2.40.1 (20161225.0304)\n" +
            " -->\n" +
            "<!-- Title: g Pages: 1 -->\n" +
            "<svg";

    @Rule
    public TemporaryFolder dotFolder = new TemporaryFolder();

    @AfterClass
    public static void end() {
        Graphviz.useEngine(null);
    }

    @Test
    public void jdk() {
        Graphviz.useEngine(new GraphvizJdkEngine());
        assertThat(Graphviz.fromString("graph g {a--b}").render(SVG_STANDALONE).toString(), startsWith(START1_4));
    }

    @Test
    public void server() {
        GraphvizServerEngine.stopServer();
        try {
            Graphviz.useEngine(new GraphvizServerEngine());
            assertThat(Graphviz.fromString("graph g {a--b}").render(SVG_STANDALONE).toString(), startsWith(START1_7));
        } finally {
            GraphvizServerEngine.stopServer();
        }
    }

    @Test
    public void v8() {
        Graphviz.useEngine(new GraphvizV8Engine());
        assertThat(Graphviz.fromString("graph g {a--b}").render(SVG_STANDALONE).toString(), startsWith(START1_7));
    }

    @Test
    public void cmdLine() throws IOException, InterruptedException {
        // Setup fake 'dot' command in env-path
        final File dotFile = this.dotFolder.newFile(GraphvizCmdLineEngine.CMD_DOT);
        dotFile.setExecutable(true);

        // Setup CommandExecutor Mocks
        final ICommandExecutor cmdExecutor = mock(ICommandExecutor.class);
        when(cmdExecutor.execute(any(CommandLine.class), any(File.class)))
                .thenAnswer(invocationOnMock -> {
                    final File workingDirectory = invocationOnMock.getArgumentAt(1, File.class);

                    final File svgInput = new File(getClass().getClassLoader().getResource("outfile1.svg").getFile());
                    final File svgOutputFile = new File(workingDirectory.getAbsolutePath() + "/outfile.svg");
                    Files.copy(svgInput.toPath(), svgOutputFile.toPath());

                    return 0;
                });

        final String envPath = dotFile.getParent();

        Graphviz.useEngine(new GraphvizCmdLineEngine(null, envPath, cmdExecutor));

        assertThat(Graphviz.fromString("graph g {a--b}").render(SVG_STANDALONE).toString(), startsWith(START1_4));

    }
}
