package org.intermine.web.logic.pathqueryresult;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.TableHelper;
import org.intermine.web.logic.results.WebResults;

/**
 * Helper for everything related to PathQueryResults
 * 
 * @author "Xavier Watkins"
 */
public class PathQueryResultHelper
{
    /**
     * For a given type, return the default view as configured in webconfig-model.xml
     * as a List of Path objects
     *  
     * @param type the type for which to get the view
     * @param model the Model
     * @param webConfig the WebConfig
     * @param prefix non-mandatory. Useful when getting a view for a reference or collection
     * @return a List of Paths representing the view
     */
    public static List<Path> getDefaultView(String type, Model model, WebConfig webConfig, 
                          String prefix, boolean excludeNonAttributes) {
        List<Path> view = new ArrayList<Path>();
        Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(
            TypeUtil.instantiate(model.getPackageName() + "." + type));
        for (ClassDescriptor classDescriptor : classDescriptors) {
            List<FieldConfig> fieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig,
                classDescriptor);
            for (FieldConfig fieldConfig : fieldConfigs) {
                if (fieldConfig.getShowInResults()) {
                    String expr = fieldConfig.getFieldExpr();
                    if (prefix == null || prefix.length() <= 0) {
                        prefix = type;
                    }
                    String pathString = prefix + "." + expr;
                    Path path = new Path(model, pathString);
                    // Path path = MainHelper.makePath(model, pathQuery, pathString);
                    // TODO remove isOnlyAttribute when outer joins
                    if (!view.contains(path)
                                    && ((excludeNonAttributes && path.isOnlyAttribute())
                                    || (!excludeNonAttributes))) { 
                        view.add(path);
                    }
                }
            }
        }
        return view;
    }
    
    /**
     * Create a PathQuery to get the contents of an InterMineBag
     * 
     * @param imBag the bag
     * @param webConfig the WebConfig
     * @param model the Model
     * @return a PathQuery
     */
    public static PathQuery makePathQueryForBag(InterMineBag imBag, WebConfig webConfig, 
                                                Model model) {
        PathQuery pathQuery = new PathQuery(model);

        List<Path> view = PathQueryResultHelper.getDefaultView(imBag.getType(), model, webConfig,
            null, true);

        pathQuery.setView(view);
        String label = null, id = null, code = pathQuery.getUnusedConstraintCode();
        Constraint c = new Constraint(ConstraintOp.IN, imBag.getName(), false, label, code, id,
                                      null);
        pathQuery.addNode(imBag.getType()).getConstraints().add(c);
        pathQuery.setConstraintLogic("A and B and C");
        pathQuery.syncLogicExpression("and");
        return pathQuery;
    }
    
    /**
     * Create a PathQuery to get results for a collection of items from an InterMineObject
     * 
     * @param webConfig the WebConfig
     * @param model the Model
     * @param id the InterMineObject identifier
     * @param referencedClassName the collection type
     * @param objectClassName the InterMineObject type
     * @param field the name of the field for the collection in the InterMineObject
     * @return a PathQuery
     */
    public static PathQuery makePathQueryForCollection(WebConfig webConfig, Model model,
                                                        Integer id, String referencedClassName,
                                                        String objectClassName, String field) {
        PathQuery pathQuery = new PathQuery(model);

        List<Path> view = PathQueryResultHelper.getDefaultView(referencedClassName, model,
                        webConfig, objectClassName + "." + field, false);

        pathQuery.setView(view);
        String label = null, id2 = null, code = pathQuery.getUnusedConstraintCode();
        Constraint c = new Constraint(ConstraintOp.EQUALS, id, false, label, code, id2, null);
        pathQuery.addNode(objectClassName + "." + "id").getConstraints().add(c);
        pathQuery.setConstraintLogic("A and B and C");
        pathQuery.syncLogicExpression("and");
        return pathQuery;
    }
    
    /**
     * Runs a PathQuery and return a WebResults object
     * 
     * @param pathQuery the PathQuery to run
     * @param profile the user Profile
     * @param os the ObjectStore
     * @param classKeys the ClassKeys
     * @param bagQueryConfig the BagQueryConfig
     * @param returnBagQueryResults populated by MainHelper.makeQuery
     * @param servletContext the ServletContext
     * @return a WebResult object
     * @throws ObjectStoreException exception thrown
     */
    public static WebResults createPathQueryGetResults(PathQuery pathQuery, Profile profile,
                                                    ObjectStore os, Map classKeys,
                                                    BagQueryConfig bagQueryConfig,
                                                    Map returnBagQueryResults,
                                                    ServletContext servletContext)
                    throws ObjectStoreException {
        Model model = os.getModel();
        Map<String, QueryNode> pathToQueryNode = new HashMap<String, QueryNode>();
        Map<String, InterMineBag> allBags = WebUtil.getAllBags(profile.getSavedBags(),
                        servletContext);
        Query query = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode, servletContext,
                        returnBagQueryResults, false, os, classKeys, bagQueryConfig);
        Results results = TableHelper.makeResults(os, query);
        WebResults webResults = new WebResults(pathQuery, results, model, pathToQueryNode,
                        classKeys, null);
        return webResults;
    }
    
    /**
     * Runs a PathQuery and return a WebResults object
     * 
     * @param pathQuery the PathQuery to run
     * @param profile the user Profile
     * @param os the ObjectStore
     * @param classKeys the ClassKeys
     * @param bagQueryConfig the BagQueryConfig
     * @param servletContext the ServletContext
     * @return a WebResult object
     * @throws ObjectStoreException exception thrown
     */
    public static WebResults createPathQueryGetResults (PathQuery pathQuery, Profile profile,
                                                    ObjectStore os, Map classKeys,
                                                    BagQueryConfig bagQueryConfig,
                                                    ServletContext servletContext)
                    throws ObjectStoreException {
        return createPathQueryGetResults(pathQuery, profile, os, classKeys, bagQueryConfig, null,
                        servletContext);
    }
    
}
