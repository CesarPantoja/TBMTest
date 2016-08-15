/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.uk.qmul.mmv.tbmtest;

import ac.uk.qmul.mmv.tbm.model.TBMFocalElement;
import ac.uk.qmul.mmv.tbm.model.TBMModel;
import ac.uk.qmul.mmv.tbm.model.TBMModelFactory;
import ac.uk.qmul.mmv.tbm.model.TBMPotential;
import ac.uk.qmul.mmv.tbm.model.TBMVarDomain;
import java.util.logging.Logger;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;

/**
 *
 * @author Cesar
 */
public class Main {

    private static void printPotential(TBMPotential p) {
        System.out.println("Potential: " + p);
        p.listFocalElements().forEachRemaining(f -> {

            System.out.println("\tFocalElement: " + f);
            System.out.println("\t\tMass: " + f.getMass());
            System.out.println("\t\tDomain: ");
            f.getDomain().listVariables().forEachRemaining(var -> System.out.println("\t\t" + var));
            System.out.println("\t\tConfigs:");
            f.listAllConfigurations().forEachRemaining(conf -> {
                System.out.println("\t\t\t" + conf);
                System.out.println("\t\t\tElements: " + conf.listAllElements().toSet().size());
                conf.listAllElements().forEachRemaining(el -> System.out.println("\t\t\t\t" + el));
            });
        });
    }

    public static void main(String[] args) {

        String datasetPath = "db";
        //initialise the TDB storage
        Dataset dataset = TDBFactory.createDataset(datasetPath);
        
        //Create the model
        //TBMModel model = TBMModelFactory.createTBMModel(dataset.getDefaultModel());      
        //For a memory-backed storage use: 
        TBMModel model = TBMModelFactory.createTBMModel(null); 

        //Namespace of the ontology
        String ont = "http://mmv.eecs.qmul.ac.uk/CLUE.owl";
        model.read("file:CLUE.owl");
        
        //instantiate variables
        //murderer variable
        Resource murderer = model.getResource(ont + "#Murderer"); //Type
        Resource ColMustard = model.getResource(ont + "#ColonelMustard");
        //room variable
        Resource room = model.getResource(ont + "#Room"); //Type
        Resource Kitchen = model.getResource(ont + "#Kitchen");
        //weapon variable
        Resource weapon = model.getResource(ont + "#Weapon"); //Type
        Resource Candlestick = model.getResource(ont + "#CandleStick");
        
        //first potential***********************
        //instantiate p1 domain
        TBMVarDomain d1 = model.createDomain();
        d1.addVariable(murderer);
        d1.addVariable(room);
        //create first focal element
        TBMFocalElement fe11 = model.createFocalElement();
        fe11.setDomain(d1);
        fe11.addConfiguration(ColMustard, Kitchen);
        fe11.setMass(0.8);
        //create second focal element
        TBMFocalElement fe12 = model.createFocalElement();
        fe12.setDomain(d1);
        fe12.addAllConfigurations();
        fe12.setMass(0.2);
        //create potential with masses
        TBMPotential player1 = model.createPotential();
        player1.setDomain(d1);
        player1.addFocalElement(fe11);
        player1.addFocalElement(fe12);

        //second potential ********************
        //instantiate p1 domain
        TBMVarDomain d2 = model.createDomain();
        d2.addVariable(murderer);
        d2.addVariable(weapon);
        //create first focal element
        TBMFocalElement fe21 = model.createFocalElement();
        fe21.setDomain(d2);
        fe21.addConfiguration(ColMustard, Candlestick);
        fe21.setMass(0.8);
        //create second focal element
        TBMFocalElement fe22 = model.createFocalElement();
        fe22.setDomain(d2);
        fe22.addAllConfigurations();
        fe22.setMass(0.2);
        //create potential with masses
        TBMPotential player2 = model.createPotential();
        player2.setDomain(d2);
        player2.addFocalElement(fe21);
        player2.addFocalElement(fe22);

        //Combine the two potentials
        TBMPotential finalPotential = model.combine(ont + "#finalPotential", player1, player2);

        //Instantiate global domain
        TBMVarDomain d = model.createDomain();
        d.addVariable(murderer);
        d.addVariable(room);
        d.addVariable(weapon);       

        //query the system and print
        TBMFocalElement query = model.createFocalElement(ont + "#queryFocalElement");
        query.setDomain(d);
        query.addConfiguration(ColMustard, Kitchen, Candlestick);        
        
        if (model.supportsTransactions()) {
            model.commit();
        }

        System.out.println("Belief: " + finalPotential.bel(query));
        System.out.println("Plausibility: " + finalPotential.pls(query));
        System.out.println("Doubt: " + finalPotential.dou(query));
        System.out.println("Ignorance: " + finalPotential.ign(query));

        /*output:
            Belief: 0.64
            Plausability: 1
            Doubt: 0
            Ignorance: 0.36
         */
        String queryString
                = "PREFIX TBM: <http://mmv.eecs.qmul.ac.uk/TBM.owl#>\n"
                + "PREFIX CLUE: <http://mmv.eecs.qmul.ac.uk/CLUE.owl#>\n"
                + "SELECT ?p ?fe ?bel ?pls ?dou ?ign\n"
                + "WHERE {\n"
                + "	bind(CLUE:finalPotential as ?p) .\n"
                + "	bind(CLUE:queryFocalElement as ?fe) .\n"
                + "       bind(TBM:bel(?p, ?fe) as ?bel)\n"
                + "       bind(TBM:pls(?p, ?fe) as ?pls)\n"
                + "       bind(TBM:dou(?p, ?fe) as ?dou)\n"
                + "       bind(TBM:ign(?p, ?fe) as ?ign)\n"
                + "}";
        
        // Execute the query and obtain results
        Query q = QueryFactory.create(queryString);
        try (QueryExecution qe = QueryExecutionFactory.create(q, model)) {
            ResultSet results = qe.execSelect();
            // Output query results
            ResultSetFormatter.out(System.out, results, q);
        }

    }
    private static final Logger LOG = Logger.getLogger(Main.class.getName());
}
