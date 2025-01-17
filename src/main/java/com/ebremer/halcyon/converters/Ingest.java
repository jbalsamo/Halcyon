package com.ebremer.halcyon.converters;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.ebremer.beakgraph.rdf.BeakWriter;
import com.ebremer.halcyon.HalcyonSettings;
import com.ebremer.ns.EXIF;
import com.ebremer.ns.HAL;
import com.ebremer.rocrate4j.ROCrate;
import com.ebremer.rocrate4j.writers.ZipWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.arrow.memory.OutOfMemoryException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class Ingest {
        
    public void OneFile(File source, File dest, boolean optimize) {
        Engine engine;
        try {
            ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new ZipWriter(dest));
            Resource rde = builder.getRDE();
            Model m = Engine.Load(source,builder.getRDE());
            engine = new Engine(m, optimize);            
            try (BeakWriter bw = new BeakWriter(builder, "halcyon")) {
                bw.Register(m);
                bw.CreateDictionary();
                engine.HilbertPhase(bw);
                System.out.println("Process Triples : "+m.size());
                bw.Add(m);
                bw.Create(builder);
                rde.getModel().add(Engine.getMeta(m, rde));
                Model whack = Engine.getHalcyonFeatures(m, rde);
                rde.getModel().add(whack);
                rde
                    .addProperty(RDF.type, HAL.HalcyonROCrate)
                    .addLiteral(EXIF.width,engine.getWidth())
                    .addLiteral(EXIF.height,engine.getHeight());
                //System.out.println("Closing BeakWriter...");
            } catch (java.lang.IllegalStateException ex) {
                System.out.println("AA : "+ex.toString());
            } catch (OutOfMemoryException ex) {
                System.out.println("BB : "+ex.toString());
                Logger.getLogger(Ingest.class.getName()).log(Level.SEVERE, null, ex);
            }
            builder.build();
        } catch (IOException ex) {
            System.out.println("File does not exist : "+source.toString());
        }
    }
    
    public void Process(File src, boolean optimize, File dest) throws FileNotFoundException, IOException { 
        if (!dest.exists()) {
            dest.mkdirs();
        }
        if (src.isDirectory()) {
            Path path = src.toPath();
            Files
                .walk(path)
                .filter(f->!f.toFile().isDirectory())
                .filter(f->{
                    return f.toString().endsWith(".ttl.gz");
                })
                .forEach(p->{
                    System.out.println(p);
                    String nd = p.toString();
                    nd = nd.substring(0,nd.length()-".ttl.gz".length())+".zip";
                    Path nnd = Path.of(nd);
                    nnd = Path.of(dest.toString(),src.toPath().relativize(nnd).toString());
                    File nndf = nnd.toFile();
                    if (!nndf.exists()) {
                        OneFile(p.toFile(), nnd.toFile(), optimize);
                    } else {
                        System.out.println("Already exists : "+nndf.toString());
                    }
                });
        } else if (src.toString().endsWith(".ttl.gz")) {
            OneFile(src, dest, optimize);
        }
    }
   
    public static void main(String[] args) throws FileNotFoundException, IOException {     
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.OFF);
        IngestParameters params = new IngestParameters();   
        JCommander jc = JCommander.newBuilder().addObject(params).build();
        jc.setProgramName("ingest");    
        try {
            jc.parse(args);
            if (params.isHelp()) {
                jc.usage();
                System.exit(0);
            } else {
                if (params.src.exists()) {
                    new Ingest().Process(params.src, params.optimize, params.dest);
                } else {
                    System.out.println("Source does not exist! "+params.src);
                }
            }
        } catch (ParameterException ex) {
            if (params.version) {
                System.out.println("ingest - Version : "+HalcyonSettings.VERSION);
            } else {
                System.out.println(ex.getMessage());
            }
        }
    }
}
