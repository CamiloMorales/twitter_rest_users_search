package external.services;

import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.OAuth;
import play.libs.OAuth.OAuthCalculator;
import play.libs.OAuth.ConsumerKey;
import play.libs.OAuth.RequestToken;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;
import com.fasterxml.jackson.databind.JsonNode;
import common.Functions;

public class TwitterOAuthService implements OAuthService
{
	private final String consumerKey;
	private final String consumerSecret;
	private final ConsumerKey key;
	private final OAuth oauthHelper;
	
	public TwitterOAuthService(String consumerKey, String consumerSecret)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.key = new ConsumerKey(consumerKey, consumerSecret);
		this.oauthHelper = new OAuth(new ServiceInfo(
				"https://api.twitter.com/oauth/request_token",
				"https://api.twitter.com/oauth/access_token",
				"https://api.twitter.com/oauth/authorize",
				 this.key
				));
	}
	
	@Override
	public Tuple<String, RequestToken> retrieveRequestToken(String callbackUrl)
	{
		RequestToken rt = oauthHelper.retrieveRequestToken(callbackUrl);				
		return new Tuple<String, RequestToken>(oauthHelper.redirectUrl(rt.token), rt);
	}

	@Override
	public Promise<JsonNode> getSearchResults(RequestToken token, String authVerifier, String query_string)
	{
		RequestToken accessToken = oauthHelper.retrieveAccessToken(token, authVerifier);
		WSRequestHolder request = WS.url("https://api.twitter.com/1.1/users/search.json?q="+query_string.replace(" ", "%20")+"&count=1") //This is a bug in the play-java-ws API, thats why I have to include manually the query on the query.
				.setQueryParameter("q", query_string) //This seems not to work because of the bug in play-java-ws although it doesnt throw any errors.
				.setQueryParameter("count", "1");

		WSRequestHolder request_authenticated = request.sign(new OAuthCalculator(key, accessToken));

		Promise<Response> result = request_authenticated.get();
		Promise<JsonNode> promiseOfJson = result.map(Functions.responseToJson);

		return promiseOfJson;	
	}
}
