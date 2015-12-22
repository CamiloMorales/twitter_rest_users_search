package controllers;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.ConfigFactory;
import external.services.OAuthService;
import external.services.TwitterOAuthService;
import play.api.libs.json.Json;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.OAuth.RequestToken;
import play.mvc.Controller;
import play.mvc.Result;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Application extends Controller
{    
	private static final OAuthService service = new TwitterOAuthService(
			ConfigFactory.load().getString("consumer.key"),
			ConfigFactory.load().getString("consumer.secret")
			);
    
    public static Result searchTwitter()
    {
    	String callbackUrl = routes.Application.searchTwitterCallback().absoluteURL(request());
    	
    	System.out.println("callbackUrl: "+callbackUrl);
    	
    	Tuple<String, RequestToken> t = service.retrieveRequestToken(callbackUrl);
    	flash("request_token", t._2.token);
    	flash("request_secret", t._2.secret);
    	
    	Map<String, String[]> params = Controller.request().queryString();
    	String search = params.get("query")[0];

    	flash("search", search);
    	return redirect(t._1);
    }
    
    public static Result searchTwitterCallback()
    {
    	RequestToken token = new RequestToken(flash("request_token"), flash("request_secret"));
    	String authVerifier = request().getQueryString("oauth_verifier");  	
    	String search = flash("search");
  
    	Promise<JsonNode> search_results_json = service.getSearchResults(token, authVerifier, search);    	
    	String json_results = search_results_json.get().toString();
    	String json_final = json_results.toString().substring(1, json_results.toString().length()-1);
    	
    	System.out.println(Json.prettyPrint(Json.parse(json_final)));
    	
    	OutputStream rdf_output = new ByteArrayOutputStream();
    	OutputStream jsonld_output = new ByteArrayOutputStream();
    	
    	String final_foaf_rdf = new String();
		String final_jsonld = new String();
    	
    	try 
    	{
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(json_final);

			String firstName =  (String) jsonObject.get("name");
			String screen_name =  (String) jsonObject.get("screen_name");		
			String profile_image_url =  (String) jsonObject.get("profile_image_url");
			String url =  (String) jsonObject.get("url");
			
			String personURI    = "http://somewhere.uni-bon.de/"+screen_name;

			Graph graph = GraphFactory.createJenaDefaultGraph();			
			
			Model model = ModelFactory.createModelForGraph(graph);
			
			model.setNsPrefix( "foaf", "http://xmlns.com/foaf/0.1/" );
			model.setNsPrefix( "fuhsen", "http://unibonn.eis.de/fuhsen/common_entities#" );			
			model.setNsPrefix( "gr", "http://purl.org/goodrelations/v1#" );			
			model.setNsPrefix( "omv", "http://omv.ontoware.org/2005/05/ontology#" );
			model.setNsPrefix( "org", "http://www.w3.org/ns/org#" );
			model.setNsPrefix( "owl", "http://www.w3.org/2002/07/owl#" );
			model.setNsPrefix( "prov", "http://www.w3.org/ns/prov#" );			
			model.setNsPrefix( "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
			model.setNsPrefix( "rdfs", "http://www.w3.org/2000/01/rdf-schema#" );
			model.setNsPrefix( "xsd", "http://www.w3.org/2001/XMLSchema#" );

			Resource res = model.createResource(personURI).addProperty(RDF.type, FOAF.Agent).addProperty(RDF.type, FOAF.Person)
								.addProperty(FOAF.homepage, model.createResource(url).addProperty(RDF.type, XSD.anyURI))							
								.addProperty(FOAF.depiction, model.createResource(profile_image_url).addProperty(RDF.type, XSD.anyURI))							
								.addProperty(FOAF.name, firstName)
				         		.addProperty(FOAF.accountName, screen_name);
				
			model.write(rdf_output, "RDF/XML");			
			final_foaf_rdf = rdf_output.toString();
			
			model.write(jsonld_output, "JSON-LD");			
			final_jsonld = jsonld_output.toString();
			
		}
    	catch (Exception ex)
    	{
			ex.printStackTrace();
		}
    	
    	return ok(views.html.searchResults.render(final_foaf_rdf, Json.prettyPrint(Json.parse(final_jsonld)))); 
    }
    
}