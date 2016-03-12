/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.uk.qmul.mmv.tbmtest;

import ac.uk.qmul.mmv.tbm.arq.TBM_Belief;
import ac.uk.qmul.mmv.tbm.arq.TBM_Doubt;
import ac.uk.qmul.mmv.tbm.arq.TBM_Ignorance;
import ac.uk.qmul.mmv.tbm.arq.TBM_Plausibility;
import ac.uk.qmul.mmv.tbm.model.TBMFocalElement;
import ac.uk.qmul.mmv.tbm.model.TBMModel;
import ac.uk.qmul.mmv.tbm.model.TBMModelFactory;
import ac.uk.qmul.mmv.tbm.model.TBMPotential;
import ac.uk.qmul.mmv.tbm.model.TBMVarDomain;
import ac.uk.qmul.mmv.tbm.vocabulary.TBM;
import java.io.BufferedReader;
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
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;

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

    private static void LoadAnnotations(String movie, TBMModel model) {

        model.add(model.createResource(NS + movie), RDF.type, model.createResource(NS + "MediaItem"));

        LoadBlood(movie, model);
        //LoadFights(movie, model);
        /* LoadFire(movie, model);
         LoadFirearms(movie, model);
         LoadColdArms(movie, model);
         LoadCarChases(movie, model);
         LoadGoryScene(movie, model);
         LoadGunshots(movie, model);
         LoadExplosions(movie, model);
         LoadScreams(movie, model);     */
    }    

   private static void LoadFights(String movie, TBMModel model) {
        List<String> lines = getResource(movie + "_fights.txt");

        //movie and property resources
        Resource movieRes = model.createResource(NS + movie);
        Property hasFrame = model.createProperty(NS + "hasFrame");
        Property hasFrameNumber = model.createProperty(NS + "hasFrameNumber");
        Resource FightRes = model.createResource(NS + "Fight");
        Resource Event = model.createResource(NS + "Event");
        Property hasGlobalPotential = model.createProperty(NS+"hasGlobalPotential");
        
        TBMVarDomain d = model.createDomain();
        d.addVariable(Event);

        for (String line : lines) {
            String[] parts = line.split(" ");
            int startFrame = Integer.parseInt(parts[0]);
            int endFrame = Integer.parseInt(parts[1]);

            for (int i = startFrame; i <= endFrame; i++) {

                //this will create the frame Resource if needed
                Resource frameRes = model.createResource(NS + movie + "/frame/" + i);
                
                //if it didn't exist before, create properties
                if(!movieRes.hasProperty(hasFrame)){
                    
                    //adds the type and frame number
                    frameRes.addProperty(RDF.type, model.createResource(NS + "Frame"))
                            .addLiteral(hasFrameNumber, i);

                    //link frame to movie
                    movieRes.addProperty(hasFrame, frameRes);
                }
                
                //create focal element (knowledge being received)
                TBMFocalElement fe = model.createFocalElement();
                fe.setDomain(d);
                fe.addConfiguration(FightRes);
                fe.setMass(1);
                
                //create tmp potential with mass (in   being received currently)
                TBMPotential currFramePotential = model.createPotential();
                currFramePotential.setDomain(d);
                currFramePotential.addFocalElement(fe);
                
                TBMPotential oldGlobPotential;
                //if frame doesn't have global potential, create potential with 
                // complete ignorance (mass 1 to the entire frame of discenrment)
                if (!frameRes.hasProperty(hasGlobalPotential)){
                    //create focal element
                    TBMFocalElement glob = model.createFocalElement();
                    glob.setDomain(d);
                    glob.addAllConfigurations();
                    glob.setMass(1);

                    //create potential with masses
                    oldGlobPotential = model.createPotential();
                    oldGlobPotential.setDomain(d);
                    oldGlobPotential.addFocalElement(glob);
                    
                }
                else {
                    
                    oldGlobPotential = frameRes.getPropertyResourceValue(hasGlobalPotential).as(TBMPotential.class);
                }
                
                //combine with global potential
                TBMPotential newGlobPotential = model.combine(oldGlobPotential, currFramePotential);
                               
                
                frameRes.removeAll(hasGlobalPotential);
                frameRes.addProperty(hasGlobalPotential, newGlobPotential);
                
                
                
                //delete tmp FE and Potential
                currFramePotential.remove();
                
                //delete oldGlobPotential and its FEs
                oldGlobPotential.remove();
                
                /*System.out.println("AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER ");
                
                newGlobPotential.listFocalElements().forEachRemaining((FE) ->
                    {
                        System.out.println("**********");
                        FE.listProperties().forEachRemaining(P -> System.out.println(P.toString()));
                        FE.listAllConfigurations().forEachRemaining(C -> {
                            System.out.print("----- ");
                            C.listAllElements().forEachRemaining(R -> System.out.print(R+" "));
                            System.out.println();
                        });
                    });*/
            }
        }
    }
    
    private static void LoadBlood(String movie, TBMModel model) {
        List<String> lines = getResource(movie + "_blood.txt");

        //movie and property resources
        Resource movieRes = model.createResource(NS + movie);
        Property hasFrame = model.createProperty(NS + "hasFrame");
        Property hasFrameNumber = model.createProperty(NS + "hasFrameNumber");
        Resource bloodRes = model.createResource(NS + "PresenceOfBlood");
        Resource Event = model.createResource(NS + "Event");
        Property hasGlobalPotential = model.createProperty(NS+"hasGlobalPotential");
        
        TBMVarDomain d = model.createDomain();
        d.addVariable(Event);

        for (String line : lines) {
            String[] parts = line.split(" ");
            int startFrame = Integer.parseInt(parts[0]);
            int endFrame = Integer.parseInt(parts[1]);

            for (int i = startFrame; i <= endFrame; i++) {

                //this will create the frame Resource if needed
                Resource frameRes = model.createResource(NS + movie + "/frame/" + i);
                
                //if it didn't exist before, create properties
                if(!movieRes.hasProperty(hasFrame)){
                    
                    //adds the type and frame number
                    frameRes.addProperty(RDF.type, model.createResource(NS + "Frame"))
                            .addLiteral(hasFrameNumber, i);

                    //link frame to movie
                    movieRes.addProperty(hasFrame, frameRes);
                }
                
                //create focal element (knowledge being received)
                TBMFocalElement fe = model.createFocalElement();
                fe.setDomain(d);
                fe.addConfiguration(bloodRes);
                fe.setMass(1);
                
                //create tmp potential with mass (in   being received currently)
                TBMPotential currFrameBlood = model.createPotential();
                currFrameBlood.setDomain(d);
                currFrameBlood.addFocalElement(fe);
                
                TBMPotential oldGlobPotential;
                //if frame doesn't have global potential, create potential with 
                // complete ignorance (mass 1 to the entire frame of discenrment)
                if (!frameRes.hasProperty(hasGlobalPotential)){
                    //create focal element
                    TBMFocalElement glob = model.createFocalElement();
                    glob.setDomain(d);
                    glob.addAllConfigurations();
                    glob.setMass(1);

                    //create potential with masses
                    oldGlobPotential = model.createPotential();
                    oldGlobPotential.setDomain(d);
                    oldGlobPotential.addFocalElement(glob);
                    
                }
                else {
                    
                    oldGlobPotential = frameRes.getPropertyResourceValue(hasGlobalPotential).as(TBMPotential.class);
                }
                
                //combine with global potential
                TBMPotential newGlobPotential = model.combine(oldGlobPotential, currFrameBlood);
                               
                
                frameRes.removeAll(hasGlobalPotential);
                frameRes.addProperty(hasGlobalPotential, newGlobPotential);
                
                
                
                //delete tmp FE and Potential
                currFrameBlood.remove();
                
                //delete oldGlobPotential and its FEs
                
                oldGlobPotential.remove();
                
                /*System.out.println("AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER AFTER ");
                
                newGlobPotential.listFocalElements().forEachRemaining((FE) ->
                    {
                        System.out.println("**********");
                        FE.listProperties().forEachRemaining(P -> System.out.println(P.toString()));
                        FE.listAllConfigurations().forEachRemaining(C -> {
                            System.out.print("----- ");
                            C.listAllElements().forEachRemaining(R -> System.out.print(R+" "));
                            System.out.println();
                        });
                    });*/
            }
        }
    }

    public static void main(String[] args) {

        //calculate co-ocurrence
        //
        
        String ont = "http://mmv.eecs.qmul.ac.uk/ontologies/violentSceneDetection.owl";
        //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);
        TBMModel model = TBMModelFactory.createTBMModel(null);
        model.read("file:data/violentSceneDetection.owl");

        LoadAnnotations("Armageddon", model);

        model.write(System.out);

    }

    private static void printPotential(TBMPotential p) {
        System.out.println("Potential: "+p);
        p.listFocalElements().forEachRemaining(f -> {

            System.out.println("\tFocalElement: " + f);
            System.out.println("\t\tMass: " + f.getMass());
            System.out.println("\t\tDomain: ");
            f.getDomain().listVariables().forEachRemaining(var -> System.out.println("\t\t" + var));
            System.out.println("\t\tConfigs:");
            f.listAllConfigurations().forEachRemaining(conf -> {
                System.out.println("\t\t\t" + conf);
                System.out.println("\t\t\tElements: "+conf.listAllElements().toSet().size());
                conf.listAllElements().forEachRemaining(el -> System.out.println("\t\t\t\t" + el));
            });
        });
    }
        
    public static void main1(String[] args) {

        String directory = "D:/db/Dataset1" ;
        Dataset dataset = TDBFactory.createDataset(directory) ;

        dataset.begin(ReadWrite.WRITE);

        //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);
        
        TBMModel model = TBMModelFactory.createTBMModel(dataset.getDefaultModel());
        //model = dataset.getDefaultModel() ;
        dataset.end() ;

        String ont = "http://mmv.eecs.qmul.ac.uk/CLUE.owl";
        //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);
        model.read("file:data/CLUE.owl");

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

         //instantiate global domain
        TBMVarDomain d = model.createDomain();
        d.addVariable(murderer);
        d.addVariable(room);
        d.addVariable(weapon);

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
        /*fe12.listAllConfigurations().forEachRemaining(C -> {
         System.out.println("-----");
         C.listAllElements().forEachRemaining(R -> System.out.print(R+" "));
         System.out.println();
         });*/

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
        /*System.out.println("***************************");
         fe22.listAllConfigurations().forEachRemaining(C -> {
         System.out.println("-----");
         C.listAllElements().forEachRemaining(R -> System.out.print(R+" "));
         System.out.println();
         });-*/

        //create potential with masses
        TBMPotential player2 = model.createPotential();
        player2.setDomain(d2);
        player2.addFocalElement(fe21);
        player2.addFocalElement(fe22);
        //model.prepare();        
        //.prepare? do the actual reasoning? combine manually?
        TBMPotential finalPotential = model.combine(ont+"#finalPotential", player1, player2);
        
        FunctionRegistry.get().put(TBM.uri+"bel", TBM_Belief.class) ;
        FunctionRegistry.get().put(TBM.uri+"pls", TBM_Plausibility.class) ;
        FunctionRegistry.get().put(TBM.uri+"dou", TBM_Doubt.class) ;
        FunctionRegistry.get().put(TBM.uri+"ign", TBM_Ignorance.class) ;
        player1.remove();
        player2.remove();
        
        //printPotential(finalPotential);
        
        /*finalPotential.listFocalElements().forEachRemaining((FE) ->
         {
         System.out.println("**********");
         FE.listProperties().forEachRemaining(P -> System.out.println(P.toString()));
         FE.listAllConfigurations().forEachRemaining(C -> {
         System.out.println("-----");
         C.listAllElements().forEachRemaining(R -> System.out.print(R+" "));
         System.out.println();
         });
         }
         );*/
        
        //query the system and print
        TBMFocalElement query = model.createFocalElement(ont+"#queryFocalElement");
        query.setDomain(d);
        query.addConfiguration(ColMustard, Kitchen, Candlestick);
        
        //dataset.end() ;

        //dataset.begin(ReadWrite.READ) ;
        
        System.out.println("belief: " + finalPotential.bel(query));
        System.out.println("Plausability: " + finalPotential.pls(query));
        System.out.println("Douobt: " + finalPotential.dou(query));
        //double com = finalPotential.com(query);
        System.out.println("Ignorance: " + finalPotential.ign(query));
        
        String queryString =
                "PREFIX TBM: <http://mmv.eecs.qmul.ac.uk/TBM.owl#>\n" +
                "PREFIX CLUE: <http://mmv.eecs.qmul.ac.uk/CLUE.owl#>\n" +
                "SELECT ?p ?fe ?bel ?pls ?dou ?ign\n" +
                "WHERE {\n" +
                "	bind(CLUE:finalPotential as ?p) .\n" +
                "	bind(CLUE:queryFocalElement as ?fe) .\n" +
                "       bind(TBM:bel(?p, ?fe) as ?bel)\n"+
                "       bind(TBM:pls(?p, ?fe) as ?pls)\n"+
                "       bind(TBM:dou(?p, ?fe) as ?dou)\n"+
                "       bind(TBM:ign(?p, ?fe) as ?ign)\n"+
                "}" ;
        
        
        //String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
        
        Query q = QueryFactory.create(queryString);

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(q, model);
        ResultSet results = qe.execSelect();

        // Output query results	
        ResultSetFormatter.out(System.out, results, q);

        // Important - free up resources used running the query
        qe.close();
        
        //dataset.end() ;
        
        
        //model.write(System.out);
    }
}
