package eu.abdn.owlaip3.tableau.reasoner.alc;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;


/** 
 * @author Yuan Ren
 * @version 2012-09-20: 
 * initial version;
 */
public class ALCReasoner {

	protected Set<OWLOntology> importClosure;
	public final OWLOntologyManager manager;
	protected final OWLDataFactory factory;
	

	public ALCReasoner(OWLOntologyManager manager, OWLOntology ontology) throws CloneNotSupportedException {
		this.manager = manager;
		this.factory = manager.getOWLDataFactory();
        this.importClosure = ontology.getImportsClosure();
	}
	
	public boolean isConsistent() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		Tableau tableau = new Tableau(importClosure, factory);
		return tableau.check();
	}

	public boolean isSatisfiable(OWLClassExpression arg0) throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		
		Tableau tableau = new Tableau(importClosure, factory);
		
		tableau.add(factory.getOWLAnonymousIndividual(), arg0);

		return tableau.check();
	}
	
	public boolean isEntailed(OWLSubClassOfAxiom axiom) throws CloneNotSupportedException
	{
		Tableau tableau = new Tableau(importClosure, factory);
		tableau.add(factory.getOWLAnonymousIndividual(), factory.getOWLObjectIntersectionOf(axiom.getSubClass(),axiom.getSuperClass().getComplementNNF()));
		return !tableau.check();
	}
	
	public boolean isEntailed(OWLClassAssertionAxiom axiom) throws CloneNotSupportedException
	{
		Tableau tableau = new Tableau(importClosure, factory);
		tableau.add(axiom.getIndividual(), axiom.getClassExpression().getComplementNNF());
		return !tableau.check();
		
	}

}
