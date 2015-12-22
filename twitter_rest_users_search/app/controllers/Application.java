package controllers;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import external.services.OAuthService;
import external.services.TwitterOAuthService;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.OAuth.RequestToken;
import play.mvc.Controller;
import play.mvc.Result;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Application extends Controller
{    
	private static final OAuthService service = new TwitterOAuthService(
			"ETHtNspalsDQo70I9wLCSt5nX",
			"IiPEHtoCaw7oYBN3MHcLt45OqxJ0qpIMkuqxjDegLIpdryHA9j"
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

    	//System.out.println("RESULTS: "+json_results);

    	String json_final = json_results.toString().substring(1, json_results.toString().length()-1);
    	
    	System.out.println("FINAL RESULTS: "+json_final);
    	
    	
    	
    	
    	OutputStream os = new ByteArrayOutputStream();
    	
    	
    	try 
    	{
			// read the json file
//			FileReader reader = new FileReader(filePath);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(json_final);

			// get a String from the JSON object
			String firstName =  (String) jsonObject.get("name");
			System.out.println("The first name is: " + firstName);
			
			String screen_name =  (String) jsonObject.get("screen_name");
			System.out.println("The screen_name is: " + screen_name);
			
			// some definitions
			String personURI    = "http://somewhere/"+screen_name;
			String foaf_name    = firstName;

			// create an empty Model
			Model model = ModelFactory.createDefaultModel();

			// create the resource
			//   and add the properties cascading style
			Resource johnSmith
			  = model.createResource(personURI)
			         .addProperty(FOAF.name, foaf_name)
			         .addProperty(FOAF.family_name, screen_name);
//			         .addProperty(FOAF.N,
//			                      model.createResource()
//			                           .addProperty(VCARD.Given, givenName)
//			                           .addProperty(VCARD.Family, familyName));
			
			
			
			model.write(os, "JSON-LD");
			
			System.out.println("FINAL_OUTPUT: "+os.toString());

		}
    	catch (Exception ex)
    	{
			ex.printStackTrace();
		}
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	return ok(views.html.searchResults.render(os.toString()));
    }
}
