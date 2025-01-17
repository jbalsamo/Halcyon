package com.ebremer.halcyon.wicket;

import com.ebremer.ethereal.LDModel;
import com.ebremer.ethereal.RDFDetachableModel;
import com.ebremer.ethereal.RDFRenderer;
import com.ebremer.ethereal.SelectDataProvider;
import com.ebremer.ethereal.Solution;
import com.ebremer.halcyon.datum.Patterns;
import com.ebremer.ns.HAL;
import com.ebremer.ethereal.NodeColumn;
import com.ebremer.halcyon.datum.DataCore;
import static com.ebremer.halcyon.datum.DataCore.Level.OPEN;
import com.ebremer.halcyon.datum.HalcyonPrincipal;
import com.ebremer.halcyon.gui.HalcyonSession;
import com.ebremer.halcyon.pools.AccessCache;
import com.ebremer.halcyon.pools.AccessCachePool;
import com.ebremer.multiviewer.MultiViewer;
import com.ebremer.ns.EXIF;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.arq.querybuilder.handlers.ValuesHandler;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

public class ListImages extends BasePage {
    private final ListFeatures lf;
    
    public ListImages() {
        System.out.println("Building a ListImages()...");
        List<IColumn<Solution, String>> columns = new LinkedList<>();
        columns.add(new NodeColumn<>(Model.of("File URI"),"s","s"));
        columns.add(new NodeColumn<>(Model.of("MD5"),"md5","md5"));
        columns.add(new NodeColumn<>(Model.of("width"),"width","width"));
        columns.add(new NodeColumn<>(Model.of("height"),"height","height"));
        //columns.add(new NodeColumn<>(Model.of("Collection"),"collection","collection"));
        columns.add(new AbstractColumn<Solution, String>(Model.of("Viewer")) {
            @Override
            public void populateItem(Item<ICellPopulator<Solution>> cellItem, String componentId, IModel<Solution> model) {
                cellItem.add(new ActionPanel(componentId, model));
            }
        });
        ParameterizedSparqlString pss = new ParameterizedSparqlString(
            """
            select distinct ?s ?width ?height ?md5
            where {
                graph ?car {?s so:isPartOf ?collection}
                graph ?s {?s a so:ImageObject;
                            owl:sameAs ?md5;
                            exif:width ?width;
                            exif:height ?height
                }
            }
            """
        );
        pss.setNsPrefix("owl", OWL.NS);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        pss.setNsPrefix("exif", EXIF.NS);
        pss.setIri("car", HAL.CollectionsAndResources.getURI());
        //Dataset ds = DatabaseLocator.getDatabase().getSecuredDataset(OPEN);
        Dataset ds = DatabaseLocator.getDatabase().getDataset();
        SelectDataProvider rdfsdf = new SelectDataProvider(ds,pss.toString());
        pss.setIri("collection", "urn:halcyon:nocollections");
        rdfsdf.SetSPARQL(pss.toString());
        AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable<>("table", columns, rdfsdf, 25);
        add(table);
        RDFDetachableModel rdg = new RDFDetachableModel(Patterns.getALLCollectionRDF());
        LDModel ldm = new LDModel(rdg);
        DropDownChoice<Node> ddc = 
            new DropDownChoice<>("collection", ldm,
                    new LoadableDetachableModel<List<Node>>() {
                        @Override
                        protected List<Node> load() {
                            org.apache.jena.rdf.model.Model ccc = ModelFactory.createDefaultModel();
                            try {
                                HalcyonPrincipal p = HalcyonSession.get().getHalcyonPrincipal();
                                String uuid = p.getURNUUID();
                                AccessCache ac = AccessCachePool.getPool().borrowObject(uuid);
                                AccessCachePool.getPool().returnObject(uuid, ac);
                                if (ac.getCollections().size()==0) {
                                    Dataset dsx = DataCore.getInstance().getSecuredDataset(OPEN);
                                    ac.getCollections().add(Patterns.getCollectionRDF2(dsx));  
                                }
                                ccc.add(ac.getCollections());                                
                            } catch (Exception ex) {
                                Logger.getLogger(Patterns.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            List<Node> list = Patterns.getCollectionList45X(ccc);
                            //System.out.println("Load Images...Loaded.");
                            return list;
                        }
                    },
                    new RDFRenderer(rdg)
                );
        Form<?> form = new Form("form");
        add(form);
        form.add(ddc);
        form.add(new AjaxButton("goFilter") {           
            @Override
            protected void onAfterSubmit(AjaxRequestTarget target) {
                HashSet<Node> features = lf.getSelectedFeatures();
                ParameterizedSparqlString pss = rdfsdf.getPSS();
                pss.setIri("collection", ddc.getModelObject().toString());
                Query q = QueryFactory.create(pss.toString());
                if (!features.isEmpty()) {
                    WhereHandler wh = new WhereHandler(q);
                    TriplePath tp = new TriplePath(Triple.create(NodeFactory.createVariable("ca"), SchemaDO.object.asNode(), NodeFactory.createVariable("md5")));
                    wh.addGraph(NodeFactory.createVariable("roc"), tp);
                    ValuesHandler vh = new ValuesHandler(q);
                    vh.addValueVar(Var.alloc("ca"), features);
                    vh.build();
                    wh.addWhere(vh);
                    rdfsdf.setQuery(q);                  
                } else {
                    rdfsdf.SetSPARQL(pss.toString());
                }
                target.add(table);
            }
        });
        lf = new ListFeatures("boo");
        add(lf);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(ListFeatures.class, "repeater.css")));
    }
    
    private class ActionPanel extends Panel {
        public ActionPanel(String id, IModel<Solution> model) {
            super(id, model);
            add(new Link<Void>("select") {               
                @Override
                public void onClick() {
                    HashSet<String>[] ff = lf.getFeatures();                    
                    Solution s = model.getObject();
                    String g = s.getMap().get("s").getURI();
                    String mv = "var images = ["+FeatureManager.getFeatures(ff[0],g)+","+FeatureManager.getFeatures(ff[1],g)+","+FeatureManager.getFeatures(ff[2],g)+","+FeatureManager.getFeatures(ff[3],g)+"]";
                    //ff[0] = new HashSet<>();
                    //ff[1] = new HashSet<>();
                    //ff[2] = new HashSet<>();
                    //ff[3] = new HashSet<>();
                    //String mv = "var images = ["+FeatureManager.getFeatures(ff[0],g)+","+FeatureManager.getFeatures(ff[1],g)+","+FeatureManager.getFeatures(ff[2],g)+","+FeatureManager.getFeatures(ff[3],g)+"]";
                    //System.out.println(mv);
                    HalcyonSession.get().SetMV(mv);
                    setResponsePage(MultiViewer.class);
                }
            });
        }
    }
}
