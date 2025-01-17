package com.ebremer.halcyon.datum;

import com.ebremer.ethereal.MakeList;
import static com.ebremer.halcyon.datum.DataCore.Level.OPEN;
import com.ebremer.halcyon.pools.AccessCache;
import com.ebremer.halcyon.pools.AccessCachePool;
import com.ebremer.ns.HAL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.graph.Node;
import org.apache.jena.permissions.model.SecuredModel;
import org.apache.jena.permissions.model.impl.SecuredModelImpl;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.SchemaDO;

/**
 *
 * @author erich
 */
public class Patterns {
    
    public static List<Node> getCollectionList() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            select ?s ?name
            where {graph ?s {?s a so:Collection; so:name ?name}}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        Dataset ds = DataCore.getInstance().getSecuredDataset();
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), ds);
        ds.begin(ReadWrite.READ);
        ResultSet rs = qe.execSelect();
        List<Node> list = MakeList.Of(rs, "s");
        ds.end();
        return list;
    }
    
    public static List<Node> getCollectionList(Model m) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            select ?s ?name
            where {?s a so:Collection; so:name ?name}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        //Dataset ds = DataCore.getInstance().getSecuredDataset(OPEN);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        //ds.begin(ReadWrite.READ);
        ResultSet rs = qe.execSelect();
        List<Node> list = MakeList.Of(rs, "s");
        //ds.end();
        return list;
    }
    
    public static List<Node> getCollectionList(Dataset ds) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            select ?s ?name
            where {graph ?s {?s a so:Collection; so:name ?name}}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), ds);
        ds.begin(ReadWrite.READ);
        ResultSet rs = qe.execSelect().materialise();
        List<Node> list = MakeList.Of(rs, "s");
        ds.end();
        return list;
    }
    
    public static Model getCollectionRDF2(Dataset ds) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            construct {?s a so:Collection}
            where {
                graph ?g {?s a so:Collection}
                graph ?s {?s a so:Collection}
            }
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        pss.setIri("g", HAL.CollectionsAndResources.getURI());
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), ds);
        ds.begin(ReadWrite.READ);
        Model m = qe.execConstruct();
        ds.end();
        return m;
    }
    
    public static Model getALLCollectionRDF() {
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            construct {?s a so:Collection; so:name ?name}
            where {graph ?g {?s a so:Collection; so:name ?name}}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        Dataset ds = DataCore.getInstance().getDataset();
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), ds);
        ds.begin(ReadWrite.READ);
        Model m = qe.execConstruct();
        ds.end();
        return m;
    }
    
    public static Model getCollectionRDF() {
        SecuredModel sec = SecuredModelImpl.getInstance(new WACSecurityEvaluator(OPEN), HAL.CollectionsAndResources.getURI()+"lstm", DataCore.getInstance().getSECM());
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            construct {?s a so:Collection; so:name ?name}
            where     {?s a so:Collection; so:name ?name}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), sec);
        Model m = qe.execConstruct();
        //System.out.println("**************************************************************************************************************************");
        //RDFDataMgr.write(System.out, m, Lang.TURTLE);
        //System.out.println("**************************************************************************************************************************");
        return m;
    }
    
    public static List<Node> getCollectionList22(String uuid) {
        System.out.println("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM");
        Model m = ModelFactory.createDefaultModel();
        try {
            AccessCache ac = AccessCachePool.getPool().borrowObject(uuid);
            m.add(ac.getCollections());
            AccessCachePool.getPool().returnObject(uuid, ac);
        } catch (Exception ex) {
            Logger.getLogger(Patterns.class.getName()).log(Level.SEVERE, null, ex);
        }
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            select ?s ?name
            where {?s a so:Collection; so:name ?name}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet rs = qe.execSelect();
        List<Node> list = MakeList.Of(rs, "s");
        System.out.println("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM -----> "+list.size());
        return list;
    }
    
    public static List<Node> getCollectionList45X(Model m) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString( """
            select ?s
            where {?s a so:Collection}
        """);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        QueryExecution qe = QueryExecutionFactory.create(pss.toString(), m);
        ResultSet rs = qe.execSelect();
        return MakeList.Of(rs, "s");
    }
}
