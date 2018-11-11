package eu.abdn.owlaip3.tableau.reasoner.alc;

import java.util.*;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.*;

public class Tableau {
    private Set<OWLOntology> ontologies;
    private OWLDataFactory factory;
    private ArrayList<OWLIndividual> nodes = new ArrayList<>();
    private HashSet<OWLIndividual> blocked = new HashSet<>();

    private HashMap<OWLIndividual, HashSet<OWLClassExpression>> nodeLabels = new HashMap<>();
    private HashMap<OWLIndividual, HashMap<OWLObjectProperty, HashSet<OWLIndividual>>> edges = new HashMap<>();

    public Tableau() {
    }

    /*
     * SETUP
     * */
    public Tableau(Set<OWLOntology> ontologies, OWLDataFactory factory) {
        this.ontologies = ontologies;
        this.factory = factory;
        for (OWLOntology ontology : ontologies)
            for (OWLNamedIndividual i : ontology.getIndividualsInSignature())
                setupIndividual(i);
    }


    private void setupIndividual(OWLNamedIndividual indi) {
        if (alreadyInitialized(indi)) return;
        setupIndividialLabels(indi);
        setupIndividialProperties(indi);
    }

    private Boolean alreadyInitialized(OWLNamedIndividual indi) {
        return nodeLabels.get(indi) != null;
    }

    private void setupIndividialLabels(OWLNamedIndividual indi) {
        HashSet<OWLClassExpression> labels = new HashSet<>();
        nodes.add(indi);
        nodeLabels.put(indi, labels);
        //insert all the types of this individual in its array
        for (OWLClassExpression exp : indi.getTypes(ontologies))
            labels.add(exp.getNNF());
    }

    private void setupIndividialProperties(OWLNamedIndividual indi) {
        HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<>();
        edges.put(indi, relations);
        for (OWLOntology onto : ontologies)
            for (Entry<OWLObjectPropertyExpression, Set<OWLIndividual>> entry : indi.getObjectPropertyValues(onto).entrySet()) {
                HashSet<OWLIndividual> objects = relations.get(entry.getKey().asOWLObjectProperty());
                if (objects == null) {
                    objects = new HashSet<OWLIndividual>();
                    relations.put(entry.getKey().asOWLObjectProperty(), objects);
                }
                objects.addAll(entry.getValue());
            }
    }

    public boolean check() throws CloneNotSupportedException {
        boolean changed = true;
        while (changed) {
            int index = 0;
            OWLIndividual next;
            changed = false;
            while (index < nodes.size()) {
                next = nodes.get(index++);
                if (blocked.contains(next))
                    continue;

                // SUB rule
                if (subRule(next))
                    changed = true;

                // AND rule
                if (andRule(next))
                    changed = true;

                // FORALL rule
                if (forallRule(next))
                    changed = true;

                // detect clash
                if (clash(next)) {
                    return false;
                }

                // EXISTS rule
                if (existsRule(next))
                    changed = true;

                if (gci())
                    changed = true;

                // block offspring nodes
                if (!(next instanceof OWLNamedIndividual))
                    checkBlock(next, next);
            }
        }

        // OR rule
        return orRule();
    }

    // check offspring nodes for blocking
    private void checkBlock(OWLIndividual blockingNode, OWLIndividual blockedParent) {
        for (Map.Entry<OWLObjectProperty, HashSet<OWLIndividual>> edge : edges.get(blockedParent).entrySet()) {
            for (OWLIndividual child : edge.getValue())
                if (!blocked.contains(child) && isSubsumed(child, blockingNode))
                    block(child);
                else
                    checkBlock(blockingNode, child);
        }
    }

    private Boolean isSubsumed(OWLIndividual child, OWLIndividual parent) {
        return nodeLabels.get(parent).containsAll(nodeLabels.get(child));
    }

    // blocking offspring nodes
    private void block(OWLIndividual node) {
        blocked.add(node);
        for (Entry<OWLObjectProperty, HashSet<OWLIndividual>> objects : edges.get(node).entrySet())
            for (OWLIndividual child : objects.getValue())
                if (!blocked.add(node))
                    block(child);
    }

    // OR rule

    private boolean orRule() throws CloneNotSupportedException {
        for (OWLIndividual node : nodes)
            if (orRule(node)) return true;
        return false;
    }

    private boolean orRule(OWLIndividual node) throws CloneNotSupportedException {
        HashSet<OWLClassExpression> thisNodeLabels = nodeLabels.get(node);
        for (OWLClassExpression exp : thisNodeLabels) {
            if (!(exp instanceof OWLObjectUnionOf)) continue;

            OWLObjectUnionOf obj = (OWLObjectUnionOf) exp;
            Set<OWLClassExpression> unionMembers = obj.getOperands();
       // obj.asDisjunctSet()
            if (hasElementsInCommon(thisNodeLabels, unionMembers))
                continue;

            for (OWLClassExpression m : unionMembers) {
                Tableau newTableau = clone();
                newTableau.add(node, m);
                if (newTableau.check())
                    return true;
            }
        }
        return false;
    }

    private boolean hasElementsInCommon(Set<OWLClassExpression> node, Set<OWLClassExpression> members) {
        return !Collections.disjoint(node, members);
    }

    void add(OWLIndividual indi, OWLClassExpression exp) {
        HashSet<OWLClassExpression> labels = nodeLabels.get(indi);
        if (labels == null) {
            labels = new HashSet<OWLClassExpression>();
            nodes.add(indi);
            nodeLabels.put(indi, labels);
            HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<>();
            edges.put(indi, relations);
        }
        labels.add(exp.getNNF());


    }

    // EXISTS rule

    private boolean existsRule(OWLIndividual node) {
        boolean changed = false;
        for (OWLClassExpression exp : nodeLabels.get(node))
            if (exp instanceof OWLObjectSomeValuesFrom) {
                OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) exp;
                OWLObjectPropertyExpression role = some.getProperty();
                OWLClassExpression filler = some.getFiller();
                HashSet<OWLIndividual> objects = edges.get(node).get(role);
                if (objects == null) {
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
    private boolean forallRule(OWLIndividual node) {
        boolean changed = false;

        var toAdd = new HashMap<OWLIndividual, HashSet<OWLClassExpression>>();

        for (OWLClassExpression exp : nodeLabels.get(node))
            if (exp instanceof OWLObjectAllValuesFrom) {
                var restr = (OWLObjectAllValuesFrom) exp;
                OWLObjectPropertyExpression role = restr.getProperty(); // R
                OWLClassExpression filler = restr.getFiller(); // D
                HashSet<OWLIndividual> destinationIndividial = edges.get(node).get(role.asOWLObjectProperty());

                for (OWLIndividual child : destinationIndividial) {
                    toAdd.putIfAbsent(child, new HashSet<>());
                    if (!nodeLabels.get(child).contains(filler)) {
                        toAdd.get(child).add(filler);
                        changed = true;
                    }
                }
            }

        toAdd.forEach((k, v) -> nodeLabels.get(k).addAll(v));


        return changed;
    }

    // SUB rule
    private boolean subRule(OWLIndividual node) {
        boolean changed = false;
        HashSet<OWLClassExpression> labels = nodeLabels.get(node);

        HashSet<OWLClassExpression> toAdd = new HashSet<OWLClassExpression>();
        for (OWLClassExpression exp : labels)
            if (exp instanceof OWLClass) {
                OWLClass atomic = (OWLClass) exp;
                for (OWLClassExpression superClass : atomic.getSuperClasses(ontologies)) {
                    if (!labels.contains(superClass.getNNF())) {
                        toAdd.add(superClass.getNNF());
                        changed = true;
                    }
                }
            }

        labels.addAll(toAdd);

        return changed;
    }

    // detect clash
    private boolean clash(OWLIndividual node) {
        HashSet<OWLClassExpression> labels = nodeLabels.get(node);
        if (labels.contains(factory.getOWLNothing()))
            return true;
        for (OWLClassExpression exp : labels)
            if (labels.contains(exp.getComplementNNF()))
                return true;
        return false;
    }

    // And rule
    private boolean andRule(OWLIndividual node) {
        var toAdd = new HashSet<OWLClassExpression>();
        for (OWLClassExpression exp : nodeLabels.get(node))
            if (exp instanceof OWLObjectIntersectionOf) {
                var intersection = (OWLObjectIntersectionOf) exp;
                Set<OWLClassExpression> intersectionMembers = intersection.asConjunctSet();
                for (OWLClassExpression im : intersectionMembers)
                    if (!nodeLabels.get(node).contains(im))
                        toAdd.add(im);
            }
        nodeLabels.get(node).addAll(toAdd);
        return toAdd.size() != 0;
    }

    private boolean gci() {
        boolean changed = false;
        for (OWLOntology o : ontologies) {
            for (OWLSubClassOfAxiom subClassOfAxiom : o.getAxioms(AxiomType.SUBCLASS_OF)) {
                OWLClassExpression sub = subClassOfAxiom.getSubClass();
                OWLClassExpression sup = subClassOfAxiom.getSuperClass();
                if (!sub.isClassExpressionLiteral()) {
                    OWLObjectUnionOf newConstraint = factory.getOWLObjectUnionOf(sup, sub.getComplementNNF());
                    addToEveryIndividual(newConstraint);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void addToEveryIndividual(OWLObjectUnionOf newConstraint) {
        for (OWLIndividual i : nodes) nodeLabels.get(i).add(newConstraint);
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
        for (Entry<OWLIndividual, HashSet<OWLClassExpression>> entry : this.nodeLabels.entrySet()) {
            HashSet<OWLClassExpression> labels = new HashSet<OWLClassExpression>(entry.getValue());
            clone.nodeLabels.put(entry.getKey(), labels);
        }
        for (Entry<OWLIndividual, HashMap<OWLObjectProperty, HashSet<OWLIndividual>>> entry : this.edges.entrySet()) {
            HashMap<OWLObjectProperty, HashSet<OWLIndividual>> relations = new HashMap<OWLObjectProperty, HashSet<OWLIndividual>>();
            clone.edges.put(entry.getKey(), relations);
            for (Entry<OWLObjectProperty, HashSet<OWLIndividual>> relationEntry : entry.getValue().entrySet()) {
                HashSet<OWLIndividual> objects = new HashSet<OWLIndividual>(relationEntry.getValue());
                relations.put(relationEntry.getKey(), objects);
            }
        }
        return clone;
    }


}
