package cs3019Test;

import eu.abdn.owlaip3.tableau.reasoner.alc.ALCReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.IOException;

public class ConsistencyCheckingExampleFromArgs {

    public static void main(String[] args) throws IOException, OWLOntologyCreationException, CloneNotSupportedException {


        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        //File file = new File("owl_test_files/approved/description-logic/inconsistent001.rdf");
        File file = new File(args[1]);
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
        ALCReasoner reasoner = new ALCReasoner(manager, ontology);

        if (reasoner.isConsistent())
            System.out.println("The ontology is consistent!");
        else
            System.out.println("The ontology is inconsistent!");


    }

}
