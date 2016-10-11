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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Cesar
 */
public class Riot {

    private static final Logger LOG = Logger.getLogger(Riot.class.getName());

    public static void main(String[] args) {
        try {            
            //model = dataset.getDefaultModel() ;

            HashMap<String, HashMap<String, BitSet>> movies = new HashMap<>(); //this would have each movie, and for each concept, a bitset with the prescence of the given concept in the frame
            HashMap<String, TBMPotential> potentials = new HashMap<>(); //global potential for each concept
            HashMap<String, String> vars = new HashMap<>(); //concept name for file name and concept in the ontology

            //populate concepts
            Set<String> concepts = new HashSet<>();
            Files.lines(Paths.get("conf_riot/concepts.txt"))
                    .filter(line -> !line.startsWith("#"))
                    .forEach(concept -> {
                        String[] parts = concept.split(" ");
                        vars.put(parts[0], parts[1]);
                        concepts.add(parts[0]);
                    });

            // <editor-fold defaultstate="collapsed" desc="Training">
            // <editor-fold defaultstate="collapsed" desc="Calculate coocurrence">
            LOG.log(Level.INFO, "Calculating coocurrences");
            //calculate coocurrence
            //read concept by concept and store them in an array
            LoadMovies(movies, concepts);
            
            Set<String> movies_train = new HashSet<>();
            Files.lines(Paths.get("conf_riot/movies_train.txt"))
                    .filter(line -> !line.startsWith("#"))
                    .forEach(movie -> movies_train.add(movie));

            HashMap<String, Integer> count = new HashMap<>(); //holds the 

            concepts.forEach(concept -> count.put(concept, 0));

            HashMap<String, HashMap<String, Integer>> store = new HashMap<>();

            for (String concept : count.keySet()) {
                //System.out.println(concept);
                store.put(concept, new HashMap<>());
                for (String conc : count.keySet()) {
                    //System.out.println("\t"+conc);
                    store.get(concept).put(conc, 0);
                }
            }

            for (String movie : movies_train) {
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
            LOG.log(Level.INFO, "Computing combined potentials");
            String ont = "http://mmv.eecs.qmul.ac.uk/ontologies/riotDetection#";

            String directory = System.getProperty("user.dir");

            //LOG.log(Level.INFO, "Directory: \"{0}\"", directory);
            String datasetPath = directory + File.separator + "db";
            new File(datasetPath).mkdirs();
            Dataset dataset = TDBFactory.createDataset(datasetPath);

            try (TBMModel model = TBMModelFactory.createTBMModel(dataset.getDefaultModel())) {
                //try (TBMModel model = TBMModelFactory.createTBMModel(null)) {
                model.read("file:conf_riot/riotDetection.owl");
                Property hasFrame = model.createProperty(ont + "hasFrame");
                Property hasFrameNumber = model.createProperty(ont + "hasFrameNumber");
                //Resource Event = model.createResource(ont + "Event");
                Property hasGlobalPotential = model.createProperty(ont + "hasGlobalPotential");
                Resource frameType = model.createResource(ont + "Frame");
                //count.keySet().forEach(conceptX -> {
                //create empty global potential for concept

                //TBMModel conceptsModel = TBMModelFactory.createTBMModel(null);
                for (String conceptX : count.keySet()) {

                    try (NodeIterator iter = model.listObjectsOfProperty(model.createResource(ont + vars.get(conceptX)), hasGlobalPotential)) {
                        if (iter.hasNext()) {
                            iter.forEachRemaining(potential -> potentials.put(conceptX, potential.as(TBMPotential.class)));
                        } else {
                            //dataset.begin(ReadWrite.WRITE);

                            //LOG.log(Level.INFO, "################ Before create empty global pot {0}", conceptX);
                            TBMPotential p = createEmptyGlobalPotential(model, ont, conceptX, vars);
                            //LOG.log(Level.INFO, "After ---------------");

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

                                    //LOG.log(Level.INFO, "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$Before combine {0}", conceptY);
                                    TBMPotential combPot = model.combine(p, newP);
                                    //LOG.log(Level.INFO, "After combine");
                                    p.remove();
                                    newP.remove();
                                    p = combPot;
                                    if (model.supportsTransactions()) {
                                        model.commit();
                                    }

                                }
                                //  create potential of conceptX and conceptY
                                //  combine with global potential
                                //  assign to global potential, delete previous global potential
                            }
                            potentials.put(conceptX, p);
                            model.add(model.getResource(ont + vars.get(conceptX)), hasGlobalPotential, p);
                            //dataset.end();
                        }
                    }
                }   // </editor-fold>
                // </editor-fold>
                
                // <editor-fold defaultstate="collapsed" desc="Populate KB">
                Set<String> movies_test = new HashSet<>();
                Files.lines(Paths.get("conf_riot/movies_test.txt"))
                        .filter(line -> !line.startsWith("#"))
                        .forEach(movie -> movies_test.add(movie));
                Set<String> concepts_test = new HashSet<>();
                Files.lines(Paths.get("conf_riot/concepts_test.txt"))
                        .filter(line -> !line.startsWith("#"))
                        .forEach(concept -> concepts_test.add(concept));

                for (String movie : movies_test) {

                    
                    if (model.contains(model.createResource(ont + movie), RDF.type, model.createResource(ont + "MediaItem"))) {
                        continue;
                    }

                    //dataset.begin(ReadWrite.WRITE);
                    LOG.log(Level.INFO, movie);
                    Resource movieRes = model.createResource(ont + movie);
                    model.add(movieRes, RDF.type, model.createResource(ont + "MediaItem"));
                    

                    for (String concept : concepts_test) {

                        LOG.log(Level.INFO, "**** {0}", concept);

                        TBMVarDomain d = model.createDomain();
                        d.addVariable(model.getResource(ont + vars.get(concept)));

                        for (Integer frame : movies.get(movie).get(concept).stream().toArray()) {
                            //Integer frame = 33600;
                            /*if (frame % 24 != 0) {
                                continue;
                            }*/

                            //LOG.log(Level.INFO, "### frame {0}", frame);

                            //System.out.println("||||||||||||||||||||||||||| Frame: "+frame+" Concept: "+concept);
                            //this will create the frame Resource if needed
                            Resource frameRes = model.createResource(ont + movie + "/frame/" + frame);

                            //if it didn't exist before, create properties
                            if (!movieRes.hasProperty(hasFrame, frameRes)) {

                                //adds the type and frame number
                                frameRes.addProperty(RDF.type, frameType)
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
                            TBMPotential newGlobPotential = model.combine(oldGlobPotential, potentials.get(concept));

                            frameRes.removeAll(hasGlobalPotential);
                            frameRes.addProperty(hasGlobalPotential, newGlobPotential);

                            //delete tmp FE and Potential
                            //currFramePotential.remove();
                            //delete oldGlobPotential and its FEs
                            oldGlobPotential.remove();
                            if (model.supportsTransactions()) {
                                model.commit();
                            }
                        }
                    }
                    //dataset.end();
                }   // </editor-fold>

                //query the system and print
                List<TBMFocalElement> queryFEs = vars.entrySet().stream().map(entry -> getQueryFE(model, ont, entry.getValue(), entry.getKey())).collect(Collectors.toList());

                String heading = "FRAME," + vars.keySet().stream().collect(Collectors.joining(",")) + ",RIOT\n";
                try (FileWriter o = new FileWriter("conf_riot/res/result_.csv")) {
                    
                    o.write(heading);

                    for (String movie : movies_test) {
                        //System.out.println(movie+"************************");
                        //dataset.end();

                        Resource movieRes = model.getResource(ont + movie);

                        ExtendedIterator<Resource> framesIter = movieRes.listProperties(hasFrame).mapWith(x -> x.getResource());

                        /*System.out.println(movie+"*************************");
                        System.out.print(heading);*/

                        while (framesIter.hasNext()) {
                            Resource currFrame = framesIter.next();
                            int frameNo = currFrame.getProperty(hasFrameNumber).getInt();

                            TBMPotential p = currFrame.getProperty(hasGlobalPotential).getResource().as(TBMPotential.class);

                            //printPotential(p);
                            //double bel = p.bel(query);
                            String msg = String.valueOf(frameNo) + ","
                                    + queryFEs.stream()
                                    .map(query -> String.valueOf(p.bel(query)))
                                    .collect(Collectors.joining(","))
                                    + "," + (movies.get(movie).get("riot").get(frameNo) ? 1 : 0) + "\n";

                            //String msg = String.format("%s,%s,%s\n", frameNo,bel,);
                            o.write(msg);
                            //System.out.print(msg);
                            //System.out.println("" + frameNo + "," + bel + "," + (movies.get(movie).get("riot").get(frameNo) ? 1 : 0));
                        }
                    }
                    o.flush();

                }
                
                //dataset.begin(ReadWrite.WRITE);
                /*model.createFocalElement(ont + "queryFocalElement").remove();
                TBMFocalElement query = model.createFocalElement(ont + "queryFocalElement");
                TBMVarDomain d = model.createDomain();
                d.addVariable(model.createResource(ont + "Violence"));
                query.setDomain(d);
                query.addConfiguration(model.createResource(ont + "violence"));
                if(model.supportsTransactions()) model.commit();
                //dataset.end();
                
                Resource movie = model.getResource(ont+"Leon");
                
                ExtendedIterator<Resource> framesIter = movie.listProperties(hasFrame).mapWith(x -> x.getResource());
                
                while(framesIter.hasNext()){
                    Resource currFrame = framesIter.next();
                    int frameNo = currFrame.getProperty(hasFrameNumber).getInt();
                    
                    TBMPotential p = currFrame.getProperty(hasGlobalPotential).getResource().as(TBMPotential.class);
                    
                    //printPotential(p);
                    
                    double bel = p.bel(query);
                    
                    System.out.println(""+frameNo+","+bel+","+(movies.get("Leon").get("violence").get(frameNo)?1:0));                    
                }
                
                System.out.println("Enter query");
                Scanner scanner = new Scanner(System.in);
                StringBuilder query_string = new StringBuilder();
                String curr_line;
                while (!(curr_line = scanner.nextLine()).equals("exit")) {
                    if (curr_line.endsWith("/")) {
                        try {
                            query_string.append(curr_line.substring(0, curr_line.lastIndexOf("/"))).append("\n");

                            System.out.println(query_string.toString());
                            //dataset.begin(ReadWrite.READ);
                            Query q = QueryFactory.create(query_string.toString());

                            try ( // Execute the query and obtain results
                                    QueryExecution qe = QueryExecutionFactory.create(q, model)) {
                                if (q.isSelectType()) {
                                    ResultSet results = qe.execSelect();
                                    
                                    // Output query results
                                    ResultSetFormatter.out(System.out, results, q);
                                    // Important - free up resources used running the query
                                } else if (q.isConstructType()) {
                                    qe.execConstruct();
                                    System.out.println("SUCCESS");
                                } else {
                                    System.out.println("Query type not recognised");
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            //dataset.end();
                            query_string.setLength(0);
                            System.out.println("Enter query");
                        }

                    } else {
                        query_string.append(curr_line).append("\n");
                    }
                }*/
                //insert concepts in knowledge base
                //learn rules
            }

            System.out.println("finished");
        } catch (IOException ex) {
            Logger.getLogger(Riot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    private static TBMFocalElement getQueryFE(TBMModel model, String ont, String conceptVar, String conceptInst) {
        TBMFocalElement query = model.createFocalElement(ont + "queryFocalElement" + conceptVar);
        TBMVarDomain d = model.createDomain();
        d.addVariable(model.createResource(ont + conceptVar));
        query.setDomain(d);
        query.addConfiguration(model.createResource(ont + conceptInst));
        if (model.supportsTransactions()) {
            model.commit();
        }
        return query;
    }

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

    private static void LoadMovies(HashMap<String, HashMap<String, BitSet>> movies, Set<String> concepts) {
        try {
            Files.lines(Paths.get("conf_riot/all_movies.txt")).forEach(movie -> movies.put(movie, new HashMap<>()));
            LoadConcepts(movies, concepts);
        } catch (IOException ex) {
            Logger.getLogger(Riot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void LoadConcepts(HashMap<String, HashMap<String, BitSet>> movies, Set<String> concepts) {

        movies.keySet().parallelStream().forEach((String movie) -> {
            try {
                File xmlFile = new File("conf_riot/GT/"+movie);
                
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);
                
                XPath xpath = XPathFactory.newInstance().newXPath();
                String expression = "/viper/data/sourcefile/file/attribute[@name='NUMFRAMES']/dvalue/@value";
                int numFrames = ((Double)xpath.evaluate(expression, doc, XPathConstants.NUMBER)).intValue();
                
                String expressionConc = "/viper/data/sourcefile/object[1]/attribute[@name='###']/bvalue[@value='true']/@framespan";
                
                //concepts.parallelStream().forEach(concept -> {
                for(String concept : concepts){
                    try {
                        //System.out.println(numFrames);
                        BitSet data = new BitSet(numFrames);
                        movies.get(movie).put(concept, data);
                        
                        NodeList list = (NodeList)xpath.evaluate(expressionConc.replaceAll("###",concept), doc, XPathConstants.NODESET);
                        
                        for(int i=0;i<list.getLength();i++){
                            String[] parts = list.item(i).getNodeValue().split(":");
                            
                            int startFrame = Integer.parseInt(parts[0]);
                            int endFrame = Integer.parseInt(parts[1]);
                            
                            data.set(startFrame, endFrame + 1);
                        }
                        
                        //LoadVideoConcept(movie + "_" + concept + ".txt", data);
                    } catch (XPathExpressionException ex) {
                        Logger.getLogger(Riot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }//);
            } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException ex) {
                Logger.getLogger(Riot.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private static int TimeToFrame(double time) {
        return Math.toIntExact(Math.round(time * 24));
    }

}
