/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.uk.qmul.mmv.tbmtest;

import java.util.Scanner;

/**
 *
 * @author Cesar
 */
public class test2 {
    public static void main(String[] args){
        // create a scanner so we can read the command-line input
        Scanner scanner = new Scanner(System.in);
        StringBuilder query_string = new StringBuilder();
        String curr_line;
        while(!(curr_line = scanner.nextLine()).equals("TERMINATE")){
                if(curr_line.endsWith("/")){
                    query_string.append(curr_line.substring(0, curr_line.lastIndexOf("/")));
                    System.out.println("execute "+query_string.toString());
                    query_string.setLength(0);
                }
                else{
                    query_string.append(curr_line);
                }
            }   
    }
}
