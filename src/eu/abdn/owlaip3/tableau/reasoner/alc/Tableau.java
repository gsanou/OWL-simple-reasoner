package eu.abdn.owlaip3.tableau.reasoner.alc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;

public class Tableau {
	Set<OWLOntology> ontologies;
	OWLDataFactory factory;
	ArrayList<OWLIndividual> nodes = new ArrayList<OWLIndividual>();
	HashSet<OWLIndividual> blocked = new HashSet<OWLIndividual>();
	
	HashMap<OWLIndividual, HashSet<OWLClassExpression>> nodeLabels = new HashMap<OWLIndividual, HashSet<OWLClassExpression>>();
	HashMap<OWLIndividual, HashMap<OWLObjectProperty, HashSet<OWLIndividual>>> edges = new HashMap<OWLIndividual, HashMap<OWLObjectProperty, HashSet<OWLIndividual>>>();
	
	public Tableau()
	{
		
	}
	
	public Tableau(Set<OWLOntology> ontologies, OWLDataFactory factory)
	{
		this.ontologies = ontologies;
		this.factory = factory;
		for(OWLOntology ontology:ontologies)
		{
			for(OWLNamedIndividual indi:ontology.getIndividualsInSignature())
			{
				HashSet<OWLClassExpression> labels = nodeLabels.get(indi);
				if(labels == null)
				{
					labels = new HashSet<OWLClassExpression>();
					nodes.add(indi);
					nodeLabels.put(indi, labels);
					for(OWLClassExpression exp:indi.getTypes(ontologies))
						labels.add(exp.getNNF());
					HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<OWLObjectProperty, HashSet<OWLIndividual>>();
					edges.put(indi, relations);
					for(OWLOntology onto:ontologies)
					for(Entry<OWLObjectPropertyExpression, Set<OWLIndividual>> entry:indi.getObjectPropertyValues(onto).entrySet())
					{
						HashSet<OWLIndividual> objects = relations.get(entry.getKey().asOWLObjectProperty());
						if(objects == null)
						{
							objects = new HashSet<OWLIndividual>();
							relations.put(entry.getKey().asOWLObjectProperty(), objects);
						}
						objects.addAll(entry.getValue());
					}
					

				}
				
				
			}
		}
	}
	
	public boolean check() throws CloneNotSupportedException{
		boolean changed = true;
		while(changed)
		{
			int index = 0;
		OWLIndividual next;
		changed = false;
		do
		{
			next = nodes.get(index++);
			if(blocked.contains(next))
				continue;
				
			// SUB rule
			if(subRule(next))
				changed = true;
			
			// AND rule
			if(andRule(next))
				changed = true;

			// FORALL rule
			if(forallRule(next))
				changed = true;
			
			// detect clash
			if(clash(next))
			{
				return false;				
			}
			
			// EXISTS rule
			if(existsRule(next))
				changed = true;
		
			// block offspring nodes
			if(!(next instanceof OWLNamedIndividual))
				checkBlock(next, next);
						
			
		}
		while(index<nodes.size());
		
		}
		
		// OR rule
		
		return orRule();
	}
	
	// check offspring nodes for blocking
	void checkBlock(OWLIndividual blockingNode, OWLIndividual blockedParent){
		//todo to check
		for (Map.Entry<OWLObjectProperty, HashSet<OWLIndividual>> edge : edges.get(blockedParent).entrySet()) {
			for (OWLIndividual child : edge.getValue())
				if (!blocked.contains(child) && isSubsumed(child,blockingNode))
					block(child);
				else 
					checkBlock(blockingNode,child);
		}
	}
	Boolean isSubsumed(OWLIndividual child, OWLIndividual parent) {
		return nodeLabels.get(parent).containsAll(nodeLabels.get(child));
	}
	
	// blocking offspring nodes
	void block(OWLIndividual node)
	{
		blocked.add(node);
		for(Entry<OWLObjectProperty, HashSet<OWLIndividual>> objects:edges.get(node).entrySet())
			for(OWLIndividual child:objects.getValue())
				if(!blocked.add(node))
					block(child);
	}
	
	// OR rule
	
	boolean orRule() throws CloneNotSupportedException
	{
		for (OWLIndividual node : nodes) {
			
			HashSet<OWLClassExpression> labels = nodeLabels.get(node);
			for (OWLClassExpression exp :labels) {
				if (!(exp instanceof OWLObjectUnionOf)) continue;
				
				OWLObjectUnionOf obj = (OWLObjectUnionOf) exp;
				Set<OWLClassExpression> unionMembers = obj.getOperands(); // not sure about the method
				
				boolean memberAlreadyPresentInLabels = false;
				for (OWLClassExpression m: unionMembers) 
					if (labels.contains(m)) {
						memberAlreadyPresentInLabels = true;
						break;
					}
					
				if (memberAlreadyPresentInLabels) continue; 
				
				for (OWLClassExpression m: unionMembers) {
					Tableau newTableau = clone();
					newTableau.add(node, m);
					//newTableau.nodeLabels.get(node).add(m);
					if (newTableau.check())
						return true;
				}
				
				
			}
		}
		return false;
	}
	
	void add(OWLIndividual indi, OWLClassExpression exp)
	{
		HashSet<OWLClassExpression> labels = nodeLabels.get(indi);
		if(labels == null)
		{
			labels = new HashSet<OWLClassExpression>();
			nodes.add(indi);
			nodeLabels.put(indi, labels);
			HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<OWLObjectProperty, HashSet<OWLIndividual>>();
			edges.put(indi, relations);
		}
		labels.add(exp.getNNF());

			
	}
	
	// EXISTS rule
	
	boolean existsRule(OWLIndividual node)
	{
		boolean changed = false;
		for(OWLClassExpression exp:nodeLabels.get(node))
			if(exp instanceof OWLObjectSomeValuesFrom)
			{
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) exp;
				OWLObjectPropertyExpression role = some.getProperty();
				OWLClassExpression filler = some.getFiller();
				HashSet<OWLIndividual> objects = edges.get(node).get(role);
				if(objects == null)
				{
					changed = true;
					OWLIndividual newindi = factory.getOWLAnonymousIndividual();
					nodes.add(newindi);
					HashSet<OWLClassExpression> labels = new HashSet<OWLClassExpression>();
					labels.add(filler.getNNF());
					nodeLabels.put(newindi, labels);
					HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<OWLObjectProperty, HashSet<OWLIndividual>>();
					edges.put(newindi, relations);
					objects = new HashSet<OWLIndividual>();
					objects.add(newindi);
					edges.get(node).put(role.asOWLObjectProperty(), objects);
					
				}
			}
		return changed;
	}
	
	// FORALL rule	
	boolean forallRule(OWLIndividual node)
	{
		boolean changed = false;
		
		HashMap<OWLObjectProperty, HashSet<OWLClassExpression> > toAdd = new HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>();
		
		for (OWLClassExpression exp : nodeLabels.get(node)) {
			if (!(exp instanceof OWLObjectAllValuesFrom)) continue;
			OWLObjectAllValuesFrom restr = (OWLObjectAllValuesFrom) exp;
			OWLObjectPropertyExpression role = restr.getProperty();
			OWLClassExpression filler = restr.getFiller(); // D
			HashSet<OWLIndividual> objects = edges.get(node).get(role);
				
			for (OWLIndividual child: objects) {
				if (!nodeLabels.get(child).contains(filler)) {
					//todo add all in toAddbefore adding it to the child
					nodeLabels.get(child).add(filler);
					changed = true;
				}
			}			
		}
		return changed;
	}
	
	// SUB rule
	boolean subRule(OWLIndividual node)
	{
		boolean changed = false;
		HashSet<OWLClassExpression> labels = nodeLabels.get(node);
		
		HashSet<OWLClassExpression> toAdd = new HashSet<OWLClassExpression>();
		for(OWLClassExpression exp:labels)
			if(exp  instanceof OWLClass)
			{
				OWLClass atomic = (OWLClass) exp;
				for(OWLClassExpression superClass:atomic.getSuperClasses(ontologies))
				{
					if(!labels.contains(superClass.getNNF()))
					{
						toAdd.add(superClass.getNNF());
						changed = true;
					}
				}
			}
		
		labels.addAll(toAdd);
		
		return changed;		
	}
	
	// detect clash
	boolean clash(OWLIndividual node)
	{
		HashSet<OWLClassExpression> labels = nodeLabels.get(node);
		if(labels.contains(factory.getOWLNothing()))
			return true;
		for(OWLClassExpression exp:labels)
			if(labels.contains(exp.getComplementNNF()))
				return true;
		return false;
	}
	
	// And rule
	boolean andRule(OWLIndividual node){
		HashSet<OWLClassExpression> toAdd = new HashSet<OWLClassExpression>();
		for (OWLClassExpression exp : nodeLabels.get(node))
			if (exp instanceof OWLObjectIntersectionOf) {
				OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) exp;
				Set<OWLClassExpression> intersectionMembers =  intersection.asConjunctSet();
				for (OWLClassExpression im : intersectionMembers) 
					if (!nodeLabels.get(node).contains(im))
						toAdd.add(im);
			}
		nodeLabels.get(node).addAll(toAdd);
		return toAdd.size() != 0;
	}

	@Override
	protected Tableau clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		Tableau clone = new Tableau();
		clone.ontologies = this.ontologies;
		clone.factory = this.factory;
		clone.nodes = new ArrayList<OWLIndividual>(this.nodes);
		clone.blocked = new HashSet<OWLIndividual>(this.blocked);
		clone.nodeLabels = new HashMap<OWLIndividual, HashSet<OWLClassExpression>>();
		for(Entry<OWLIndividual, HashSet<OWLClassExpression>> entry:this.nodeLabels.entrySet())
		{
			HashSet<OWLClassExpression> labels = new HashSet<OWLClassExpression>(entry.getValue());
			clone.nodeLabels.put(entry.getKey(), labels);
		}
		for(Entry<OWLIndividual, HashMap<OWLObjectProperty, HashSet<OWLIndividual>>> entry:this.edges.entrySet())
		{
			HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<OWLObjectProperty, HashSet<OWLIndividual>>();
			clone.edges.put(entry.getKey(), relations);
			for(Entry<OWLObjectProperty, HashSet<OWLIndividual>> relationEntry:entry.getValue().entrySet())
			{
				HashSet<OWLIndividual> objects = new HashSet<OWLIndividual>(relationEntry.getValue());
				relations.put(relationEntry.getKey(), objects);
			}
		}
		return clone;
	}

	
}
