/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.uk.qmul.mmv.tbmtest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cesar
 */
public class SearchAndDestroy {
    public static void main(String[] args) {
        try {
            Files.lines(Paths.get("conf_riot/all_movies.txt")).forEach(line -> {
                try {
                    Path path = Paths.get("conf_riot/GT/"+line);
                    Charset charset = StandardCharsets.UTF_8;
                    
                    String content = new String(Files.readAllBytes(path), charset);
                    content = content.replaceAll("hooded_person", "face_covered");
                    Files.write(path, content.getBytes(charset));
                } catch (IOException ex) {
                    Logger.getLogger(SearchAndDestroy.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } catch (IOException ex) {
            Logger.getLogger(SearchAndDestroy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
