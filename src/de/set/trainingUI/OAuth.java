package de.set.trainingUI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import spark.Request;

public class OAuth {

	private final String name;
	private final String submitUrl;
	private final String clientId;
	private final String clientSecret;
	private final String userInfoUrl;
	private final String usernameField;

	public OAuth(String name, String submitUrl, String clientId, String clientSecret, String userInfoUrl, String usernameField) {
		this.name = name;
		this.submitUrl = submitUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.userInfoUrl = userInfoUrl;
		this.usernameField = usernameField;
	}

	public String login(Request request) throws IOException {
		if (System.getProperty("de.set.trainingUi.skipAuth") != null) {
			return System.getProperty("de.set.trainingUi.skipAuth");
		}
    	final String savedState = request.cookie("savedState");
        final String requestState = request.queryParams("state");
        if (savedState == null || !savedState.equals(requestState)) {
        	throw new RuntimeException("invalid state " + requestState);
        }
        final String code = request.queryParams("code");
        final String redirectUrl = request.cookie("savedRedirect");

        final URL url = new URL("https://github.com/login/oauth/access_token");
        final String postData = String.format(
        		"client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&state=%s",
        		URLEncoder.encode(this.clientId, "UTF-8"),
        		URLEncoder.encode(this.clientSecret, "UTF-8"),
				URLEncoder.encode(code, "UTF-8"),
				URLEncoder.encode(redirectUrl, "UTF-8"),
				URLEncoder.encode(savedState, "UTF-8"));
        final JsonValue tokenResponse = this.post(url, postData);
        final String token = tokenResponse.asObject().getString("access_token", "");
        if (token.isEmpty()) {
        	throw new IOException("returned token is invalid");
        }
        final JsonValue userInfo = this.get(new URL(this.userInfoUrl), token);
		return userInfo.asObject().getString(this.usernameField, "");
	}

	private JsonValue post(final URL url, final String dataToSend)
			throws IOException, ProtocolException, UnsupportedEncodingException {
		final HttpURLConnection tokenConnection = (HttpURLConnection) url.openConnection();
        tokenConnection.setRequestMethod("POST");
        tokenConnection.setDoOutput(true);
        tokenConnection.setRequestProperty("Accept", "application/json");
        try (OutputStream output = tokenConnection.getOutputStream()) {
            output.write(dataToSend.getBytes("UTF-8"));
        }
        return this.readResponse(tokenConnection);
	}

	private JsonValue get(final URL url, String token)
			throws IOException, ProtocolException, UnsupportedEncodingException {
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "token " + token);
        return this.readResponse(connection);
	}

	private JsonValue readResponse(final HttpURLConnection connection) throws IOException {
		final int responseCode = connection.getResponseCode();
        if (responseCode / 100 != 2) {
        	throw new IOException("invalid response: " + responseCode);
        }
        try (InputStream input = connection.getInputStream()) {
        	final byte[] content = input.readAllBytes();
        	return Json.parse(new String(content, "UTF-8"));
        }
	}

	public String getName() {
		return this.name;
	}

	public String getClientId() {
		return this.clientId;
	}

	public String getSubmitUrl() {
		return this.submitUrl;
	}

}
