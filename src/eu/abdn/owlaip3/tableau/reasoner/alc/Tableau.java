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

    HashSet<OWLClassExpression> globalExpressions = new HashSet<>();

    public Tableau() {
    }

    /*
     * SETUP
     * */
    public Tableau(Set<OWLOntology> ontologies, OWLDataFactory factory) {
        this.ontologies = ontologies;
        this.factory = factory;
        for (OWLOntology ontology : ontologies) {
            for (OWLNamedIndividual i : ontology.getIndividualsInSignature())
                setupIndividual(i);
            for (OWLIndividual i : ontology.getReferencedAnonymousIndividuals())
                setupIndividual(i);
        }
    }


    private void setupIndividual(OWLIndividual indi) {
        if (alreadyInitialized(indi)) return;
        setupIndividialLabels(indi);
        setupIndividialProperties(indi);
    }

    private Boolean alreadyInitialized(OWLIndividual indi) {
        return nodeLabels.get(indi) != null;
    }

    private void setupIndividialLabels(OWLIndividual indi) {
        HashSet<OWLClassExpression> labels = new HashSet<>();
        nodes.add(indi);
        nodeLabels.put(indi, labels);
        //insert all the types of this individual in its array
        for (OWLClassExpression exp : indi.getTypes(ontologies))
            labels.add(exp.getNNF());
    }

    private void setupIndividialProperties(OWLIndividual indi) {
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


                if (GCISubclassOf())
                    changed = true;

                if (equivalence())
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
        if (!edges.containsKey(blockedParent)) return;
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
        for (OWLIndividual node : nodes) {
            HashSet<OWLClassExpression> thisNodeLabels = nodeLabels.get(node);
            for (OWLClassExpression exp : thisNodeLabels) {
                if (!(exp instanceof OWLObjectUnionOf)) continue;

                OWLObjectUnionOf obj = (OWLObjectUnionOf) exp;
                Set<OWLClassExpression> unionMembers = obj.asDisjunctSet();

                if (hasElementsInCommon(thisNodeLabels, unionMembers))
                    continue;

                for (OWLClassExpression m : unionMembers) {
                    Tableau newTableau = clone();
                    newTableau.add(node, m);
                    if (newTableau.check())
                        return true;
                }
                return false;

            }
        }
        return true;
    }


    private boolean hasElementsInCommon(Set<OWLClassExpression> node, Set<OWLClassExpression> members) {
        return !Collections.disjoint(node, members);
    }

    void add(OWLIndividual indi, OWLClassExpression exp) {
        HashSet<OWLClassExpression> labels = nodeLabels.get(indi);
        if (labels == null) {
            labels = new HashSet<OWLClassExpression>();
            labels.addAll(globalExpressions);
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
                    labels.addAll(globalExpressions);
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
                if (edges.get(node) == null) continue;
                HashSet<OWLIndividual> destinationIndividial = edges.get(node).get(role.asOWLObjectProperty());
                if (destinationIndividial == null) continue;
                for (OWLIndividual child : destinationIndividial) {
                    if (!nodeLabels.get(child).contains(filler)) {
                        toAdd.putIfAbsent(child, new HashSet<>());
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
        var labels = nodeLabels.get(node);
        for (OWLClassExpression exp : labels)
            if (exp instanceof OWLObjectIntersectionOf) {
                var intersection = (OWLObjectIntersectionOf) exp;
                Set<OWLClassExpression> intersectionMembers = intersection.asConjunctSet();
                for (OWLClassExpression im : intersectionMembers)
                    if (!labels.contains(im))
                        toAdd.add(im);
            }
        labels.addAll(toAdd);
        return toAdd.size() > 0;
    }

    // it handles equivalence between 2 classes
    private boolean equivalence() {
        boolean changed = false;
        for (OWLOntology o : ontologies) {
            for (OWLAxiom ax : o.getAxioms()) {
                if (ax instanceof OWLEquivalentClassesAxiom) {

                    var eqAx = (OWLEquivalentClassesAxiom) ax;
                    for (var C : eqAx.getClassExpressions())
                        for (var D : eqAx.getClassExpressionsMinus(C)) {
                            changed |= addSubclassOf(C, D);
                            changed |= addSubclassOf(D, C);
                        }
                }
            }
        }
        return changed;
    }

    private boolean GCISubclassOf() {
        boolean changed = false;
        for (OWLOntology o : ontologies)
            for (OWLAxiom ax : o.getGeneralClassAxioms()) {
                if (ax instanceof OWLSubClassOfAxiom) {
                    var subAx = (OWLSubClassOfAxiom) ax;
                    OWLClassExpression sub = subAx.getSubClass();
                    OWLClassExpression sup = subAx.getSuperClass();
                    changed |= addSubclassOf(sup, sub);
                }
            }

        return changed;
    }

    private boolean addSubclassOf(OWLClassExpression superclass, OWLClassExpression subclass) {
        boolean changed = false;
        if (!subclass.isClassExpressionLiteral()) { // GCI
            OWLObjectUnionOf newConstraint = factory.getOWLObjectUnionOf(superclass, subclass.getComplementNNF());
            globalExpressions.add(newConstraint);
            for (OWLIndividual i : nodes)
                if (!nodeLabels.get(i).contains(newConstraint)) {
                    nodeLabels.get(i).add(newConstraint);
                    changed = true;
                }
            return changed;
        }
        //normal subclass axiom
        var newSubclassAxiom = factory.getOWLSubClassOfAxiom(subclass, superclass);
        for (var o : ontologies)
            if (!o.getAxioms().contains(newSubclassAxiom)) {
                changed = true;
                o.getOWLOntologyManager().addAxiom(o, newSubclassAxiom);
            }

        return changed;
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
