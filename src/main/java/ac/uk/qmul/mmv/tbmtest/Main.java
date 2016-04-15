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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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

    static final String NS = "http://mmv.eecs.qmul.ac.uk/ontologies/violentSceneDetection#";

    public static List<String> getResource(String resourceName) {
        try {
            InputStream in = Main.class.getClassLoader().getResourceAsStream(resourceName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            ArrayList<String> out = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                out.add(line);
            }
            return out;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

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

        String directory = "";
        if (args.length == 1) {
            directory = args[0];//+File.separator+"db";
        } else {
            directory = System.getProperty("user.dir");
        }

        LOG.log(Level.INFO, "Path separator: \"{0}\"", File.separator);

        LOG.log(Level.INFO, "Directory: \"{0}\"", directory);
        String datasetPath = directory + File.separator + "db_clue";
        new File(datasetPath).mkdirs();
        Dataset dataset = TDBFactory.createDataset(datasetPath);

        String outputFile = directory + File.separator + "result.txt";

        try ( //dataset.begin(ReadWrite.WRITE);
                //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);
                TBMModel model = TBMModelFactory.createTBMModel(dataset.getDefaultModel()) //TBMModel model = TBMModelFactory.createTBMModel(null);
                //model = dataset.getDefaultModel() ;
                ) {
            String ont = "http://mmv.eecs.qmul.ac.uk/CLUE.owl";
            //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);
            model.read("file:" + directory + File.separator + "CLUE.owl");
            //model.write(System.out);
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
            Resource Dagger = model.getResource(ont + "#Dagger");
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

            //model.prepare();
            //.prepare? do the actual reasoning? combine manually?
            if (model.getResource(ont + "#finalPotential").canAs(TBMPotential.class)) {
                model.getResource(ont + "#finalPotential").as(TBMPotential.class).remove();
            }
            TBMPotential finalPotential = model.combine(ont + "#finalPotential", player1, player2);
            if (model.supportsTransactions()) {
                model.commit();
            }

            //model.write(System.out);
            //dataset.end();
            //dataset.begin(ReadWrite.WRITE);
            //instantiate global domain
            TBMVarDomain d = model.createDomain();
            d.addVariable(murderer);
            d.addVariable(room);
            d.addVariable(weapon);
            
            printPotential(finalPotential);
            
            //query the system and print
            TBMFocalElement query = model.createFocalElement(ont + "#queryFocalElement");
            query.setDomain(d);
            query.addConfiguration(ColMustard, Kitchen, Candlestick);
            
            
           
           
            //query.addConfiguration(ColMustard, Kitchen, Dagger);
            //dataset.end() ;
            //dataset.begin(ReadWrite.READ);
            System.out.println("Belief: " + finalPotential.bel(query));
            System.out.println("Plausibility: " + finalPotential.pls(query));
            System.out.println("Doubt: " + finalPotential.dou(query));
            //double com = finalPotential.com(query);
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
            //String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
            Query q = QueryFactory.create(queryString);
            try ( // Execute the query and obtain results
                    QueryExecution qe = QueryExecutionFactory.create(q, model)) {
                ResultSet results = qe.execSelect();
                // Output query results
                //FileWriter f = new FileWriter(outputFile);
                ResultSetFormatter.out(new FileOutputStream(outputFile), results, q);
                // Important - free up resources used running the query
                //dataset.end() ;
                //model.write(System.out);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            model.removeAll();
        }
    }
    private static final Logger LOG = Logger.getLogger(Main.class.getName());
}
