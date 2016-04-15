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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Cesar
 */
public class NewClass {

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

        TBMModel model = TBMModelFactory.createTBMModel(null);
        String ont = "http://mmv.eecs.qmul.ac.uk/CLUE.owl";
        model.read("file:24.owl");

        //instantiate variables
        //murderer variable
        Resource murderer = model.getResource(ont + "#Murderer"); //Type
        Resource Tony = model.getResource(ont + "#Tony");
        Resource Nina = model.getResource(ont + "#Nina");
        Resource Jamey = model.getResource(ont + "#Jamey");

        TBMVarDomain d = model.createDomain();
        d.addVariable(murderer);
        //first potential***********************
        //instantiate p1 domain
        TBMFocalElement fe11 = model.createFocalElement();
        fe11.setDomain(d);
        fe11.addConfiguration(Tony);
        fe11.addConfiguration(Nina);
        fe11.setMass(0.7);
        
        TBMFocalElement fe12 = model.createFocalElement();
        fe12.setDomain(d);
        fe12.addAllConfigurations();
        fe12.setMass(0.3);
        
        TBMPotential p1 = model.createPotential();
        p1.setDomain(d);
        p1.addFocalElement(fe11);
        p1.addFocalElement(fe12);
        
        //second potential***********************
        //instantiate p1 domain
        TBMFocalElement fe21 = model.createFocalElement();
        fe21.setDomain(d);
        fe21.addConfiguration(Nina);
        fe21.setMass(0.4);
        
        TBMFocalElement fe22 = model.createFocalElement();
        fe22.setDomain(d);
        fe22.addConfiguration(Tony);
        fe22.addConfiguration(Jamey);
        fe22.setMass(0.6);
        
        TBMPotential p2 = model.createPotential();
        p2.setDomain(d);
        p2.addFocalElement(fe21);
        p2.addFocalElement(fe22);
        
        //printPotential(p1);
        //printPotential(p2);
        

        TBMPotential finalPotential = model.combine(ont + "#finalPotential", p1, p2);

        printPotential(finalPotential);
        
        model.removeAll();
        

    }
    private static final Logger LOG = Logger.getLogger(Main.class.getName());
}
