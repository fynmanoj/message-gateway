package org.fineract.messagegateway.sms.providers.impl.jasmin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.fineract.messagegateway.configuration.HostConfig;
import org.fineract.messagegateway.constants.MessageGatewayConstants;
import org.fineract.messagegateway.exception.MessageGatewayException;
import org.fineract.messagegateway.sms.domain.SMSBridge;
import org.fineract.messagegateway.sms.domain.SMSMessage;
import org.fineract.messagegateway.sms.providers.SMSProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

@Service(value="JasminSMS")
public class JasminSMSProvider extends SMSProvider {

    private static final Logger logger = LoggerFactory.getLogger(JasminSMSProvider.class);

    private HashMap<String, OkHttpClient> restClients = new HashMap<>() ; 
    
    private static final String SCHEME = "http";
    
    private final String callBackUrl ;
    
    @Autowired
	public JasminSMSProvider(final HostConfig hostConfig) {
		callBackUrl = String.format("%s://%s:%d/jasminsms/report/", hostConfig.getProtocol(),  hostConfig.getHostName(), hostConfig.getPort());
    	logger.info("Registering call back to jasminsms:"+callBackUrl);
	}


	@Override
	public void sendMessage(SMSBridge smsBridgeConfig, SMSMessage message) throws MessageGatewayException {
		

		OkHttpClient okHttpClient = getRestClient(smsBridgeConfig);
		String baseURL = smsBridgeConfig.getConfigValue(MessageGatewayConstants.PROVIDER_URL);
    	String providerAccountId = smsBridgeConfig.getConfigValue(MessageGatewayConstants.PROVIDER_ACCOUNT_ID) ;
    	String providerAuthToken = smsBridgeConfig.getConfigValue(MessageGatewayConstants.PROVIDER_AUTH_TOKEN) ;
		try {
			URI uri = new URIBuilder()
			        .setScheme(SCHEME)
			        .setHost(baseURL)
			        //.setPath("/send")
			        .setParameter("to", message.getMobileNumber())
			        //.setParameter("from", "")
			        //.setParameter("coding", "")
			        .setParameter("username", providerAccountId)
			        .setParameter("password", providerAuthToken)
			        .setParameter("priority", "2")
			        //.setParameter("sdt", "")  //000000000100000R (send in 1 minute)
			        //.setParameter("validity-period", "")
			        .setParameter("dlr", "yes")
			        .setParameter("dlr-url", callBackUrl)
			        .setParameter("dlr-level", "2")
			        .setParameter("dlr-method", "GET")
			        //.setParameter("tags", "")
			        .setParameter("content", message.getMessage())
			        //.setParameter("hex-content", "")
			        .build();
			HttpGet httpget = new HttpGet(uri);
			
			URL url =  httpget.getURI().toURL(); 
	
			Request request = new Request.Builder()
				   .url(url)
				   .build(); 
		
			Response response = okHttpClient.newCall(request).execute();
			
			
			message.setSubmittedOnDate(new Date());
		} catch (IOException | URISyntaxException e) {
			throw new MessageGatewayException(e.getMessage());
		}

	}
	
	
	private OkHttpClient getRestClient(final SMSBridge smsBridge) {
    	String authorizationKey = encodeBase64(smsBridge) ;
    	OkHttpClient client = this.restClients.get(authorizationKey) ;
		if(client == null) {
			client = this.get(smsBridge) ;
			this.restClients.put(authorizationKey, client) ;
		}
	    return client ;
    }
    
	OkHttpClient get(final SMSBridge smsBridgeConfig) {
    	logger.debug("Creating a new Twilio Client ....");

        final OkHttpClient client = new OkHttpClient();
        return client;
    }
    
}
