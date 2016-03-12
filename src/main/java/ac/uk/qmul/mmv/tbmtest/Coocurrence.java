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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
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
public class Coocurrence {

    public static void main(String[] args) {
        try {

            // <editor-fold defaultstate="collapsed" desc="Training">
            // <editor-fold defaultstate="collapsed" desc="Calculate coocurrence">
            //calculate coocurrence
            //read concept by concept and store them in an array
            HashMap<String, HashMap<String, BitSet>> movies = new HashMap<>();

            HashMap<String, String> vars = new HashMap<>();

            Set<String> video_concepts = new HashSet<>();
            Files.lines(Paths.get("data/video_concepts.txt")).forEach(concept -> {
                String[] parts = concept.split(" ");
                vars.put(parts[0], parts[1]);
                video_concepts.add(parts[0]);
            });

            Set<String> audio_concepts = new HashSet<>();
            Files.lines(Paths.get("data/audio_concepts.txt")).forEach(concept -> {
                String[] parts = concept.split(" ");
                vars.put(parts[0], parts[1]);
                audio_concepts.add(parts[0]);
            });

            LoadMovies(movies, video_concepts, audio_concepts);

            HashMap<String, Integer> count = new HashMap<>();

            video_concepts.forEach(concept -> count.put(concept, 0));
            audio_concepts.forEach(concept -> count.put(concept, 0));

            HashMap<String, HashMap<String, Integer>> store = new HashMap<>();

            for (String concept : count.keySet()) {
                //System.out.println(concept);
                store.put(concept, new HashMap<>());
                for (String conc : count.keySet()) {
                    //System.out.println("\t"+conc);
                    store.get(concept).put(conc, 0);
                }
            }

            for (String movie : movies.keySet()) {
                for (String concept : movies.get(movie).keySet()) {

                    for (Integer frame : movies.get(movie).get(concept).stream().toArray()) {

                        //System.out.println("\t\t"+concept);
                        count.replace(concept, count.get(concept) + 1);

                        for (String conc : movies.get(movie).keySet()) {

                            if (movies.get(movie).get(conc).get(frame)) {
                                store.get(concept).replace(conc, store.get(concept).get(conc) + 1);

                            }
                        }
                    }
                }
            }

            HashMap<String, HashMap<String, Double>> coocurrence = new HashMap<>();

            store.keySet()./*parallelStream().*/forEach(concx -> {
                        coocurrence.put(concx, new HashMap<>());
                        store.get(concx).keySet()./*parallelStream().*/forEach(concy -> {

                                    double cooc = ((double) store.get(concx).get(concy)) / ((double) count.get(concx));

                                    coocurrence.get(concx).put(concy, cooc);
                                });
                    });

            /*coocurrence.keySet().forEach(concx -> {
                System.out.println(concx);
                coocurrence.get(concx).keySet().forEach(concy -> {
                    System.out.printf("%s,%f,", concy, coocurrence.get(concx).get(concy));
                });
                System.out.println();
            });*/
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Compute combined potentials for each concept">
            String ont = "http://mmv.eecs.qmul.ac.uk/ontologies/violentSceneDetection#";
            String directory = "C:/db/Dataset2";
            Dataset dataset = TDBFactory.createDataset(directory);

            dataset.begin(ReadWrite.WRITE);

            //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);
            TBMModel model = TBMModelFactory.createTBMModel(dataset.getDefaultModel());
            //model = dataset.getDefaultModel() ;
            dataset.end();
            model.read("file:data/violentSceneDetection.owl");

            HashMap<String, TBMPotential> potentials = new HashMap<>();

            //count.keySet().forEach(conceptX -> {
            //create empty global potential for concept
            for (String conceptX : count.keySet()) {

                TBMPotential p = createEmptyGlobalPotential(model, ont, conceptX, vars);

                //printPotential(p);
                //for each other concept
                for (String conceptY : count.keySet()) {
                    if (!conceptX.equals(conceptY)) {
                        //System.out.printf("%s - %s\n", conceptX, conceptY);
                        double mass = coocurrence.get(conceptX).get(conceptY);
                        //mass=mass>0.25?mass:0;

                        TBMVarDomain d = model.createDomain();
                        d.addVariable(model.getResource(ont + vars.get(conceptX)));
                        d.addVariable(model.getResource(ont + vars.get(conceptY)));

                        TBMPotential newP = model.createPotential();
                        newP.setDomain(d);

                        if (mass != 0) {
                            TBMFocalElement fe = model.createFocalElement();
                            fe.setDomain(d);
                            fe.addConfiguration(model.getResource(ont + conceptX), model.getResource(ont + conceptY));
                            fe.setMass(mass);
                            newP.addFocalElement(fe);
                        }
                        if (mass != 1) {
                            TBMFocalElement fe1 = model.createFocalElement();
                            fe1.setDomain(d);
                            fe1.addConfiguration(model.getResource(ont + conceptX), model.getResource(ont + conceptY));
                            fe1.addConfiguration(model.getResource(ont + conceptX), model.getResource(ont + "no_" + conceptY));
                            fe1.setMass(1 - mass);
                            newP.addFocalElement(fe1);
                        }

                        TBMPotential combPot = model.combine(p, newP);
                        p.remove();
                        newP.remove();
                        p = combPot;

                    }
                    //  create potential of conceptX and conceptY
                    //  combine with global potential
                    //  assign to global potential, delete previous global potential
                }
                potentials.put(conceptX, p);
            }

            // </editor-fold>
            // </editor-fold>
            // <editor-fold defaultstate="collapsed" desc="Populate KB">
            Property hasFrame = model.createProperty(ont + "hasFrame");
            Property hasFrameNumber = model.createProperty(ont + "hasFrameNumber");
            //Resource Event = model.createResource(ont + "Event");
            Property hasGlobalPotential = model.createProperty(ont + "hasGlobalPotential");

            Set<String> movies_test = new HashSet<>();

            Files.lines(Paths.get("data/movies_test.txt"))
                    .filter(line -> !line.startsWith("#"))
                    .forEach(movie -> movies_test.add(movie));

            for (String movie : movies_test) {

                LOG.log(Level.INFO, movie);
                model.add(model.createResource(ont + movie), RDF.type, model.createResource(ont + "MediaItem"));

                Resource movieRes = model.createResource(ont + movie);

                for (String concept : movies.get(movie).keySet()) {
                    LOG.log(Level.INFO, "**** {0}", concept);

                    TBMPotential conceptPot = potentials.get(concept);

                    TBMVarDomain d = model.createDomain();
                    d.addVariable(model.getResource(ont + vars.get(concept)));

                    for (Integer frame : movies.get(movie).get(concept).stream().toArray()) {

                        if (frame % 25 != 0) {
                            continue;
                        }

                        //this will create the frame Resource if needed
                        Resource frameRes = model.createResource(ont + movie + "/frame/" + frame);

                        //if it didn't exist before, create properties
                        if (!movieRes.hasProperty(hasFrame)) {

                            //adds the type and frame number
                            frameRes.addProperty(RDF.type, model.createResource(ont + "Frame"))
                                    .addLiteral(hasFrameNumber, frame);

                            //link frame to movie
                            movieRes.addProperty(hasFrame, frameRes);
                        }

                        TBMPotential oldGlobPotential;
                        //if frame doesn't have global potential, create potential with 
                        // complete ignorance (mass 1 to the entire frame of discenrment)
                        if (!frameRes.hasProperty(hasGlobalPotential)) {
                            //create focal element                            
                            TBMFocalElement glob = model.createFocalElement();
                            glob.setDomain(d);
                            glob.addAllConfigurations();
                            glob.setMass(1);

                            //create potential with masses
                            oldGlobPotential = model.createPotential();
                            oldGlobPotential.setDomain(d);
                            oldGlobPotential.addFocalElement(glob);
                        } else {
                            oldGlobPotential = frameRes.getPropertyResourceValue(hasGlobalPotential).as(TBMPotential.class);
                        }

                        //combine with global potential
                        TBMPotential newGlobPotential = model.combine(oldGlobPotential, conceptPot);

                        frameRes.removeAll(hasGlobalPotential);
                        frameRes.addProperty(hasGlobalPotential, newGlobPotential);

                        //delete tmp FE and Potential
                        //currFramePotential.remove();
                        //delete oldGlobPotential and its FEs
                        oldGlobPotential.remove();

                    }
                }
            }

            // </editor-fold>
            FunctionRegistry.get().put(TBM.uri + "bel", TBM_Belief.class);
            FunctionRegistry.get().put(TBM.uri + "pls", TBM_Plausibility.class);
            FunctionRegistry.get().put(TBM.uri + "dou", TBM_Doubt.class);
            FunctionRegistry.get().put(TBM.uri + "ign", TBM_Ignorance.class);

            model.write(new FileWriter("data/ont.owl"));

            Scanner scanner = new Scanner(System.in);
            StringBuilder query_string = new StringBuilder();
            String curr_line;

            while (!(curr_line = scanner.nextLine()).equals("EXTERMINATE")) {
                if (curr_line.endsWith("/")) {
                    query_string.append(curr_line.substring(0, curr_line.lastIndexOf("/"))).append("\n");
                    System.out.println(query_string.toString());
                    Query q = QueryFactory.create(query_string.toString());

                    // Execute the query and obtain results
                    QueryExecution qe = QueryExecutionFactory.create(q, model);
                    ResultSet results = qe.execSelect();

                    // Output query results	
                    ResultSetFormatter.out(System.out, results, q);
                    // Important - free up resources used running the query
                    qe.close();
                    query_string.setLength(0);
                } else {
                    query_string.append(curr_line).append("\n");
                }
            }

            //insert concepts in knowledge base
            //learn rules
        } catch (IOException ex) {
            Logger.getLogger(Coocurrence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static final Logger LOG = Logger.getLogger(Coocurrence.class.getName());

    private static void printPotential(TBMPotential p) {
        System.out.println("Potential: " + p);
        p.listFocalElements().forEachRemaining(f -> {

            System.out.println("\tFocalElement: " + f);
            System.out.println("\t\tMass: " + f.getMass());
            System.out.println("\t\tDomain: ");
            f.getDomain().listVariables().forEachRemaining(var -> System.out.println("\t\t\t" + var));
            System.out.println("\t\tConfigs:");
            f.listAllConfigurations().forEachRemaining(conf -> {
                System.out.println("\t\t\t" + conf);
                System.out.println("\t\t\tElements: " + conf.listAllElements().toSet().size());
                conf.listAllElements().forEachRemaining(el -> System.out.println("\t\t\t" + el));
            });
        });
    }

    private static TBMPotential createEmptyGlobalPotential(TBMModel model, String uri, String concept, HashMap<String, String> vars) {

        Resource v = model.getResource(uri + vars.get(concept));

        //System.out.println(v.getURI());
        TBMVarDomain d = model.createDomain();
        d.addVariable(v);

        TBMFocalElement fe = model.createFocalElement();
        fe.setDomain(d);
        fe.addAllConfigurations();
        fe.setMass(1);

        //TBMPotential p = model.createPotential(uri+vars.get(concept)+"globPotential");
        TBMPotential p = model.createPotential();
        p.setDomain(d);
        p.addFocalElement(fe);

        return p;

    }

    private static void LoadMovies(HashMap<String, HashMap<String, BitSet>> movies, Set<String> video_concepts, Set<String> audio_concepts) {
        try {
            Files.lines(Paths.get("data/movies_train.txt")).forEach(movie -> movies.put(movie, new HashMap<>()));
            LoadConcepts(movies, video_concepts, audio_concepts);
        } catch (IOException ex) {
            Logger.getLogger(Coocurrence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void LoadConcepts(HashMap<String, HashMap<String, BitSet>> movies, Set<String> video_concepts, Set<String> audio_concepts) {

        movies.keySet().parallelStream().forEach(movie -> {
            video_concepts.parallelStream().forEach(concept -> {
                BitSet data = new BitSet();
                movies.get(movie).put(concept, data);
                LoadVideoConcept(movie + "_" + concept + ".txt", data);
            });

            audio_concepts.parallelStream().forEach(concept -> {
                BitSet data = new BitSet();
                movies.get(movie).put(concept, data);
                LoadAudioConcept(movie + "_" + concept + ".txt", data);
            });
        });
    }

    private static void LoadVideoConcept(String file, BitSet data) {
        try {
            Files.lines(Paths.get("data/GT/" + file)).forEach(line -> {
                if (!line.isEmpty()) {
                    String[] parts = line.split(" ");

                    int startFrame = Integer.parseInt(parts[0]);
                    int endFrame = Integer.parseInt(parts[1]);

                    data.set(startFrame, endFrame + 1);
                }

            });
        } catch (IOException ex) {
            Logger.getLogger(Coocurrence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void LoadAudioConcept(String file, BitSet data) {
        try {
            Files.lines(Paths.get("data/GT/" + file)).forEach(line -> {
                if (!line.isEmpty()) {
                    String[] parts = line.split(" ");
                    double startTime = Double.parseDouble(parts[0]);
                    double endTime = Double.parseDouble(parts[1]);

                    int startFrame = TimeToFrame(startTime);
                    int endFrame = TimeToFrame(endTime);

                    boolean value = !parts[2].equals("(nothing)");

                    data.set(startFrame, endFrame + 1, value);
                }
            });
        } catch (IOException ex) {
            Logger.getLogger(Coocurrence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int TimeToFrame(double time) {
        return Math.toIntExact(Math.round(time * 24));
    }

}
