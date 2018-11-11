package cs3019Test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class ReasonerTestRunner {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;


    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @Test
    public void testBlockingExample() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        BlockingExample.main(null);
        assertEquals("The concept is unsatisfiable!\n", outContent.toString());
    }
    @Test
    public void testConsistencyCheckingExample() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        ConsistencyCheckingExample.main(null);
        assertEquals("The ontology is inconsistent!\n", outContent.toString());
    }
    @Test
    public void testDisjunctExample() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        DisjunctionExample.main(null);
        assertEquals("The subsumption is entailed!\n", outContent.toString());
    }
    @Test
    public void testGCIExample() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        GCIExample.main(null);
        assertEquals("The class assertion is entailed!\n", outContent.toString());
    }
    @Test
    public void subsumptionCheckingExample() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        SubsumptionCheckingExample.main(null);
        assertEquals("The subsumption is entailed!\n", outContent.toString());
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}