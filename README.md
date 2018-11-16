# OWL-simple-reasoner
Very simple and incomplete reasoner that uses (an old version of) owlapi, done for an university project.
It uses the Tableau algorithm to check ontologies consistency.
This has been made public after the assessment deadline.

The interesting part is in the `Tableau.java` file.

It (should) handle:
- GCI
- Subclass rule
- AND rule
- OR rule
- Forall rule
- Exists rule
- Equivalence rule
- Block in recursive definitions