package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.OrganismPathway;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.PathwayComponent;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


/**
 * @author jcarlson
 *
 */
public class BiopaxPathwayDisplayer extends ReportDisplayer {

  Profile profile;
  PathQueryExecutor exec;
  String pathwayName = null;
  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public BiopaxPathwayDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
    super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
    HttpSession session = request.getSession();
    final InterMineAPI im = SessionMethods.getInterMineAPI(session);

    profile = SessionMethods.getProfile(session);
    exec = im.getPathQueryExecutor(profile);
    OrganismPathway pathwayObj = (OrganismPathway)reportObject.getObject();

    String json;
    HashMap<String,HashSet<String>> enzymes;
    HashMap<String,HashSet<String>> genes;
    try {
      json = getJSON(pathwayObj.getId());
      enzymes = getEnzymes(pathwayObj.getId());
      genes = getGenes(pathwayObj.getId());
    } catch (ObjectStoreException e) {
      return;
    }

    try {
      // now we need to process the pathway json template. Add the enzyme/proteins
      // and calculate positions.
      JSONObject jObj = new JSONObject(json);
      JSONArray jNodes = jObj.getJSONArray("nodes");

      int maxX = 0;
      int maxY = 0;
      // used to calculate the layout. Height is in ex's. Width is in em's
      HashMap<Integer,Integer> maxWidth = new HashMap<Integer,Integer>();
      HashMap<Integer,Integer> maxHeight = new HashMap<Integer,Integer>();
      for(int i=0; i<jNodes.length(); i++) {
        JSONObject node = jNodes.getJSONObject(i);
        Integer x = new Integer(node.getInt("x"));
        maxX = (x>maxX)?x:maxX;
        Integer y = new Integer(node.getInt("y"));
        maxY = (y>maxY)?y:maxY;
        String label = node.getString("label");

        // replace the label if this is a reaction node
        if (node.getString("type").equals("reaction")) {
          StringBuffer newLabel = new StringBuffer();
          if ( enzymes.containsKey(label) ) {
            for( String enzyme: enzymes.get(label)) {
              if (newLabel.length() > 0) newLabel.append("<br>");
              newLabel.append(enzyme);
            }
          }
          if ( genes.containsKey(label) ) {
            for( String protein: genes.get(label)) {
              if (newLabel.length() > 0) newLabel.append("<br>");
              newLabel.append(protein);
            }
          }
          label = newLabel.toString();
          node.put("label",label);
        }

        Integer width = getWidth(label);
        Integer height = getHeight(label);
        if (!maxWidth.containsKey(x) || maxWidth.get(x) < width) {
          maxWidth.put(x,width);
        }
        if (!maxHeight.containsKey(y) || maxHeight.get(y) < height) {
          maxHeight.put(y,height);
        }
      }
      // now recompute x and y. For even columns, (0,2,4...) x is the
      // sum of widths <= column number. For odd columns, x is the sum
      // of widths < column number

      // a map from row/col to placement
      HashMap<Integer,Integer> xPlace = new HashMap<Integer,Integer>();
      int xPlacement = 0;
      for(int i = 0 ; i <= maxX; i+=2) {
        if (maxWidth.containsKey(Integer.valueOf(i))) {
          xPlacement += maxWidth.get(Integer.valueOf(i));
        } else {
          // leave a gap?
          xPlacement += 5;
        }
        xPlace.put(Integer.valueOf(i),xPlacement);
        // another gap
        xPlacement += 10;
        xPlace.put(Integer.valueOf(i+1),xPlacement);
        if (maxWidth.containsKey(Integer.valueOf(i+1))) {
          xPlacement += maxWidth.get(Integer.valueOf(i+1));
        } else {
          // leave a gap?
          xPlacement += 5;
        }
      }
      // repeat for y position
      int yPlacement = 0;
      HashMap<Integer,Integer> yPlace = new HashMap<Integer,Integer>();
      for(int i = 0 ; i <= maxY; i++) {
        if (maxHeight.containsKey(Integer.valueOf(i))) {
          yPlacement += maxHeight.get(Integer.valueOf(i));
        } else {
          // leave a gap?
          yPlacement += 2;
        }
        yPlace.put(Integer.valueOf(i),yPlacement);
      }
      for(int i=0; i<jNodes.length(); i++) {
        JSONObject node = jNodes.getJSONObject(i);
        node.put("x",xPlace.get(Integer.valueOf(node.getInt("x"))));
        node.put("y",yPlace.get(Integer.valueOf(node.getInt("y"))));
      }
      request.setAttribute("jsonPathway",jObj.toString());
      if (pathwayName != null) {
        request.setAttribute("pathwayName",pathwayName);
      } else {
        request.setAttribute("pathwayName","unknown");
      }
      
      String jE = makeExpressionJSON(pathwayObj.getId());
     
      // now get the corresponding expression data
      request.setAttribute("jsonExpression",jE);
      
    } catch (Exception e) {
      // some problem retrieving data or processing the JSON
    }
  }
  private Integer getHeight(String l) {
    // the height of a label. Count <br>'s and add 1
    return new Integer(1 + l.split("<br>").length);
  }

  private Integer getWidth(String l) {
    // the width of a label.
    // first, split on the lines
    String[] lines = l.split("<br>");
    if (lines.length == 1) {
      int width = 0;
      // remove all html tags and sum of the # of characters
      // this over-estimates the width of html escapes (i.e. &alpha;)
      // and UTF-8, but, meh.
      for( String bit:  lines[0].split("<[^>]*>") ) {
        width += bit.length();
      }
      return new Integer(width);
    } else {
      // multiple lines. Look at each and return the longest
      Integer width = new Integer(0);
      for(String line: lines ) {
        Integer newWidth = getWidth(line);
        width = (newWidth > width)?newWidth:width;
      }
      return width;
    }
  }
  private String getJSON(Integer id) throws ObjectStoreException {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.json","OrganismPathway.pathway.name");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
    ExportResultsIterator result = exec.execute(query);
    String json = null;
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      json = row.get(0).getField().toString();
      pathwayName = row.get(1).getField().toString();
    }
    return json;
  }

  private HashMap<String,HashSet<String>> getEnzymes(Integer id) throws ObjectStoreException {
    HashMap<String,HashSet<String>> enzymes = new HashMap<String,HashSet<String>>();
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.components.identifier","OrganismPathway.pathway.components.ontologyTerms.identifier");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
    return makeHash(query);
  }

  private HashMap<String,HashSet<String>> getGenes(Integer id) throws ObjectStoreException {
    HashMap<String,HashSet<String>> enzymes = new HashMap<String,HashSet<String>>();
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.components.identifier",
        "OrganismPathway.pathway.components.proteins.genes.primaryIdentifier");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
    return makeHash(query);
  }

  private HashMap<String,HashSet<String>> makeHash(PathQuery q) throws ObjectStoreException {
    ExportResultsIterator result = exec.execute(q);
    HashMap<String,HashSet<String>> map = new HashMap<String,HashSet<String>>();
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      String node = row.get(0).getField().toString();
      if (!map.containsKey(node)) {
        map.put(node,new HashSet<String>());
      }
      map.get(node).add(row.get(1).getField().toString());
    }
    return map;
  }
  private String makeExpressionJSON(Integer id) throws ObjectStoreException, JSONException  {
    // query for the genes and fpkm's associated with the proteins in
    // an organismpathway with this id.
    // this is organized in order of group (first), then gene
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.components.proteins.genes.cufflinksscores.experiment.experimentGroup",
        "OrganismPathway.pathway.components.proteins.genes.primaryIdentifier",
        "OrganismPathway.pathway.components.ontologyTerms.identifier",
        "OrganismPathway.pathway.components.proteins.genes.cufflinksscores.experiment.name",
        "OrganismPathway.pathway.components.proteins.genes.cufflinksscores.fpkm");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
    // probably not necessary to set the order direction...
    query.addOrderBy("OrganismPathway.pathway.components.proteins.genes.cufflinksscores.experiment.experimentGroup", OrderDirection.ASC);
    query.addOrderBy("OrganismPathway.pathway.components.proteins.genes.primaryIdentifier", OrderDirection.ASC);
    // some reactions have no EC's
    query.setOuterJoinStatus("OrganismPathway.pathway.components.ontologyTerms", OuterJoinStatus.OUTER);
    ExportResultsIterator result = exec.execute(query);
    JSONArray jsonArr = new JSONArray();
    String currentGene = null;
    String currentGroup = null;
    // this will hold the results for 1 gene in 1 group
    // the key is the sample name and value the fpkm
    HashMap<String,Float> geneResults = null;
    // this will hold the results for all genes in 1 group
    // the key is the gene name and value the geneResults hash
    HashMap<String,HashMap<String,Float>> groupResults = null;
    // a map of gene name to EC.
    HashMap<String,HashSet<String>> ecMap = new HashMap<String,HashSet<String>>();
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      String group = row.get(0).getField().toString();
      String gene = row.get(1).getField().toString();
      if (!ecMap.containsKey(gene)) ecMap.put(gene,new HashSet<String>()); 
      if (currentGroup==null || !currentGroup.equals(group)) {
        // this is either the first pass through or different group
        if (currentGroup != null) {
          // different group. Put the gene results in the group results
          groupResults.put(currentGene,geneResults);
          // and add all old results to the JSON
          addGroupNode(jsonArr,currentGroup,groupResults,ecMap);
        }
        currentGene = gene;
        currentGroup = group;
        geneResults = new HashMap<String,Float>();
        groupResults = new HashMap<String,HashMap<String,Float>>();
      } else if (!currentGene.equals(gene)) {
        // same group, different gene.
        groupResults.put(currentGene,geneResults);
        geneResults = new HashMap<String,Float>();
        currentGene = gene;
      }
      ecMap.get(gene).add(row.get(2).getField().toString());
      //              |--------- sample name ---------|----------- fpkm -----------|
      geneResults.put(row.get(3).getField().toString(),(Float)row.get(4).getField());
    }
    if(geneResults.keySet().size() > 0) {
      // final insertion
      groupResults.put(currentGene,geneResults);
      // and add all old results to the JSON
      addGroupNode(jsonArr,currentGroup,groupResults,ecMap);
    }
    JSONObject jj = new JSONObject();
    jj.put("data",jsonArr);
    return jj.toString();
  }
  private void addGroupNode(JSONArray j, String groupName, HashMap<String,HashMap<String,Float>> results,HashMap<String,HashSet<String>> ecMap) throws JSONException {
    JSONObject groupNode = new JSONObject();
    groupNode.put("group",groupName);
    JSONArray geneArray = new JSONArray();
    for(String g: results.keySet()) {
      JSONObject geneNode = new JSONObject();
      geneNode.put("gene",g);
      geneNode.put("enzyme",StringUtils.join(ecMap.get(g)," "));
      JSONArray expArray = new JSONArray();
      HashMap<String,Float> fpkm = results.get(g);
      for(String e: fpkm.keySet()) {
        JSONObject n = new JSONObject();
        n.put("sample",e);
        n.put("fpkm",fpkm.get(e));
        expArray.put(n);
      }
      geneNode.put("samples",expArray);
      geneArray.put(geneNode);
    }
    groupNode.put("genes",geneArray);
    j.put(groupNode);
  }
}
