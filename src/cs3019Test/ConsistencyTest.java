package cs3019Test;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ConsistencyTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(2);

    @Parameterized.Parameter
    public String path;


    @Parameterized.Parameters
    public static Iterable<? extends Object> data() {
        final String basePath = "owl_test_files/approved/description-logic";
        File folder = new File(basePath);
        File[] listOfFiles = folder.listFiles();
        List<File> inconsistentOntologies = Arrays.stream(listOfFiles).filter(f -> f.getName().contains("consistent") && !f.getName().contains("in") && f.getName().contains("rdf")).collect(Collectors.toList());
        String[] res = new String[inconsistentOntologies.size()];
        for (int i = 0; i < inconsistentOntologies.size(); i++) {
            res[i] = basePath + "/" + inconsistentOntologies.get(i).getName();
        }
        return Arrays.asList(res);
    }

    @Test
    public void consistency() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        String[] args = new String[2];
        args[0] = "";
        args[1] = this.path;
        originalOut.println("trying with "+this.path);
        ConsistencyCheckingExampleFromArgs.main(args);
        assertEquals("The ontology is consistent!\n", outContent.toString());
    }


    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }


    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}