/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsvfilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 *
 * @author n_otsuka
 */
public class Parameter extends Properties {
    
    public void load(File parameterFile) {

        try (InputStream is = new FileInputStream(parameterFile)) {
            loadFromXML(is);
        } catch (FileNotFoundException ex) {
//            logger.error("TsTracker property load failed:", e);
            ex.printStackTrace();
        } catch (IOException ex) {
//            logger.error("TsTracker property load failed:", e);
            ex.printStackTrace();
        }

    }

    public void store(File parameterFile) {

        try (OutputStream os = new FileOutputStream(parameterFile)) {
            storeToXML(os, null);
        } catch (FileNotFoundException ex) {
//            logger.error("TsTracker property save failed:", e1);
            ex.printStackTrace();
        } catch (IOException ex) {
//            logger.error("TsTracker property save failed:", e1);
            ex.printStackTrace();
        }

    }

    

    
}
