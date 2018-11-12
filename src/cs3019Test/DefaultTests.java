package cs3019Test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DefaultTests {
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
        assertEquals("The concept is satisfiable!\n", outContent.toString());
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
    public void testAssessment() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        ConsistencyCheckingExampleFromArgs.main(new String[]{"", "owl_test_files/computer.owl"});
        assertEquals("The ontology is consistent!\n", outContent.toString());
    }
    @Test
    public void subsumptionCheckingExample() throws OWLOntologyCreationException, CloneNotSupportedException, IOException {
        SubsumptionCheckingExample.main(null);
        assertEquals("The subsumption is entailed!\n", outContent.toString());
    }

    @Test
    public void inconsistency() {
        File folder = new File("owl_test_files/approved/description-logic");
        File[] listOfFiles = folder.listFiles();
        List<File> inconsistentOntologies =  Arrays.stream(listOfFiles).filter(f -> f.getName().contains("inconsistent")).collect(Collectors.toList());
    }


    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}