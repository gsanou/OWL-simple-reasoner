package cs3019Test;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.abdn.owlaip3.tableau.reasoner.alc.ALCReasoner;

public class ConsistencyCheckingExample {

    public static void main(String[] args) throws IOException, OWLOntologyCreationException, CloneNotSupportedException {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File("owl_test_files/ConsistencyCheckingExample.owl");
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
        ALCReasoner reasoner = new ALCReasoner(manager, ontology);

        if (reasoner.isConsistent())
            System.out.println("The ontology is consistent!");
        else
            System.out.println("The ontology is inconsistent!");


    }

}
