package cs3019Test;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import eu.abdn.owlaip3.tableau.reasoner.alc.ALCReasoner;

public class SubsumptionCheckingExample {
	
	public static void main(String[] args) throws IOException, OWLOntologyCreationException, CloneNotSupportedException {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		File file = new File("SubsumptionCheckingExample.owl");
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
		ALCReasoner reasoner = new ALCReasoner(manager, ontology);
		
		OWLDataFactory factory = manager.getOWLDataFactory();

		OWLClass Person = factory.getOWLClass(IRI.create("http://www.abdn.ac.uk/cs3019/subsumptionCheckingExample.owl#Person"));
		OWLClass Confucian = manager.getOWLDataFactory().getOWLClass(IRI.create("http://www.abdn.ac.uk/cs3019/subsumptionCheckingExample.owl#Confucian"));
		OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(Confucian, Person);
		if(reasoner.isEntailed(axiom))
		System.out.println("The subsumption is entailed!");
		else
			System.out.println("The subsumption is not entailed!");
	}

}
