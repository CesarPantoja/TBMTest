/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.uk.qmul.mmv.tbmtest;

import ac.uk.qmul.mmv.tbm.model.TBMConfiguration;
import ac.uk.qmul.mmv.tbm.model.TBMFocalElement;
import ac.uk.qmul.mmv.tbm.model.TBMModel;
import ac.uk.qmul.mmv.tbm.model.TBMModelFactory;
import ac.uk.qmul.mmv.tbm.model.TBMPotential;
import ac.uk.qmul.mmv.tbm.model.TBMVarDomain;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;

/**
 *
 * @author Cesar
 */
public class perfTest {

    public static void main(String[] args) throws IOException {

        /*args[0] = "D:/db/";
        args[1] = "data/";*/
        FileWriter o = new FileWriter("result.csv");
        //test(2, 12, 2, 2, 1000, 1000, 100, 100, o);
        //test(2, 2, 500, 1000, 100, 100, 100, 100, o);
        //test(2, 2, 500, 500, 100, 700, 100, 100, o);
        //test(2, 2, 1000, 1000, 200, 200, 100, 250, o);
        test(11, 11, 2, 2, 2, 100, 100, 100, o);
        //test(11, 11, 2, 2, 2, 100, 100, 100, o);
        o.close();
    }

    public static void test(
            //String dataSet,
            int MIN_NUM_VARIABLES,
            int MAX_NUM_VARIABLES,
            int MIN_NUM_INSTANCES,
            int MAX_NUM_INSTANCES,
            int MIN_FOCAL_ELEMENTS,
            int MAX_FOCAL_ELEMENTS,
            int MIN_CONFIGURATIONS,
            int MAX_CONFIGURATIONS,
            FileWriter o) throws IOException {

        //////////////
        Random rn = new Random();
        //StringBuilder o = new StringBuilder();

        String URI = "http://mmv.eecs.qmul.ac.uk/ontologies/performanceStudy#";
        //Reasoner reasoner = TBMReasonerFactory.theInstance().create(null);

        o.write("************************************************************\n");
        o.write("numVars,numInstances,numFocalElements,numConfigs,time\n");
        //System.out.print("**************************************************\n");
        //System.out.print("numVars,numInstances,numFocalElements,numConfigs,time\n");

        for (int numVars = MIN_NUM_VARIABLES; numVars <= MAX_NUM_VARIABLES; numVars++) {
            for (int numInstances = MIN_NUM_INSTANCES; numInstances <= MAX_NUM_INSTANCES; numInstances++) {
                for (int numFocalElements = MIN_FOCAL_ELEMENTS; numFocalElements <= MAX_FOCAL_ELEMENTS; numFocalElements++) {
                    for (int numConfigs = MIN_CONFIGURATIONS; numConfigs <= MAX_CONFIGURATIONS; numConfigs++) {

                        String directory = System.getProperty("user.dir");

                        //LOG.log(Level.INFO, "Directory: \"{0}\"", directory);
                        String datasetPath = directory + File.separator + "db";
                        new File(datasetPath).mkdirs();
                        Dataset dataset = TDBFactory.createDataset(datasetPath);

                        TBMModel model = TBMModelFactory.createTBMModel(dataset.getDefaultModel());
                        //TBMModel model = TBMModelFactory.createTBMModel(null);

                        TBMVarDomain d = model.createDomain();

                        //create variables
                        for (int i = 0; i < numVars; i++) {

                            Resource var = model.createResource(URI + "var" + i);

                            d.addVariable(var);

                            //create instances
                            for (int j = 0; j < numInstances; j++) {
                                model.createResource(URI + "var" + i + "inst" + j, var);
                            }
                        }

                        if (model.supportsTransactions()) {
                            model.commit();
                        }

                        ////Potential 1////
                        TBMPotential p1 = model.createPotential();
                        p1.setDomain(d);
                        for (int i = 0; i < numFocalElements; i++) {

                            TBMFocalElement fe = model.createFocalElement();
                            fe.setDomain(d);
                            fe.setMass(((double) 1) / (double) numFocalElements);
                            p1.addFocalElement(fe);

                            for (int j = 0; j < numConfigs; j++) {
                                TBMConfiguration config = model.createConfiguration();
                                for (int k = 0; k < numVars; k++) {
                                    config.addElement(model.getResource(URI + "var" + k + "inst" + (rn.nextInt(numInstances))));
                                }
                                fe.addConfiguration(config);
                            }
                        }

                        ////Potential 2////
                        TBMPotential p2 = model.createPotential();
                        p2.setDomain(d);
                        for (int i = 0; i < numFocalElements; i++) {

                            TBMFocalElement fe = model.createFocalElement();
                            fe.setDomain(d);
                            fe.setMass(((double) 1) / (double) numFocalElements);
                            p2.addFocalElement(fe);

                            for (int j = 0; j < numConfigs; j++) {
                                TBMConfiguration config = model.createConfiguration();
                                for (int k = 0; k < numVars; k++) {
                                    config.addElement(model.getResource(URI + "var" + k + "inst" + (rn.nextInt(numInstances))));
                                }
                                fe.addConfiguration(config);
                            }
                        }

                        if (model.supportsTransactions()) {
                            model.commit();
                        }
                        
                        LOG.log(Level.INFO, "Before combine {0}", numFocalElements);

                        //System.out.println("numVars,memory,time");
                        long startTime = System.currentTimeMillis();

                        // working code here
                        TBMPotential combPotential = model.combine(p1, p2);

                        if (model.supportsTransactions()) {
                            model.commit();
                        }

                        long stopTime = System.currentTimeMillis();
                        LOG.log(Level.INFO, "After combine");
                        long elapsedTime = (stopTime - startTime);
                        //model.removeAll();
                        // Get the Java runtime
                        //Runtime runtime = Runtime.getRuntime();
                        // Run the garbage collector
                        //runtime.gc();
                        // Calculate the used memory
                        //long memory = runtime.totalMemory() - runtime.freeMemory();

                        //System.out.println("numVars,numInstances,numFocalElements,numConfigs,memory,time");
                        o.write(String.format("%d,%d,%d,%d,%d\n", numVars, numInstances, numFocalElements, numConfigs, /*memory,*/ elapsedTime));
                        o.flush();
                        //model.close();
                        //System.out.printf("%d,%d,%d,%d,%d\n", numVars, numInstances, numFocalElements, numConfigs, /*memory,*/ elapsedTime);

                        /*try {
                            model.write(new FileWriter("data/test"+numVars+".owl"));
                        } catch (IOException ex) {
                            Logger.getLogger(perfTest.class.getName()).log(Level.SEVERE, null, ex);
                        }*/
                    }
                }
            }
        }

        System.out.println("Finished");
        //o.flush();

        //create focal elements
        //create configurations
        //combine
        //measure time and memory
    }
    private static final Logger LOG = Logger.getLogger(perfTest.class.getName());

}
