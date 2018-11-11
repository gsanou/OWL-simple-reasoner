package cs3019Test;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.abdn.owlaip3.tableau.reasoner.alc.ALCReasoner;

public class BlockingExample {

    public static void main(String[] args) throws IOException, OWLOntologyCreationException, CloneNotSupportedException {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File("blockingExample.owl");
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
        ALCReasoner reasoner = new ALCReasoner(manager, ontology);

        OWLClass Person = manager.getOWLDataFactory().getOWLClass(IRI.create("http://www.abdn.ac.uk/cs3019/blockingExample.owl#Person"));
        if (reasoner.isSatisfiable(Person))
            System.out.println("The concept is satisfiable!");
        else
            System.out.println("The concept is unsatisfiable!");
    }

}
