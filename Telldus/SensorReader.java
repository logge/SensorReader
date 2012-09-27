
import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




public class SensorReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if(args.length < 4 || (args.length > 4 && args.length < 9) ) {
			System.out.println("SensorReader is used to retrieve sensor data from Telldus Live! and then upload it to a ftp server");
			System.out.println("Public and private keys can be retrieved from Telldus website");
			System.out.println("To retrieve sensor ids:");
			System.out.println("SensorReader.java PublicKey PrivateKey PublicToken SecretToken");
			System.out.println("To start retrieving data:");
			System.out.println("SensorReader.java PublicKey PrivateKey PublicToken SecretToken");
			System.out.println("FtpServer FtpUser FtpPassword ReadIntervalInMinutes sensorId1 sensorId2 ... sensorIdN");
			return;
		}
		
		String publicKey   = args[0];
		String privateKey  = args[1];
		String publicToken = args[2];
		String secretToken = args[3];
		
		if(args.length == 4) {
			
			// Create OAuth service.
			OAuthService lobj_OAuthService = new ServiceBuilder()
			                .provider(GoogleApi.class)                                
			                .apiKey(publicKey)                                
			                .apiSecret(privateKey)   
			                /*.debug()*/
			                .build();

			// Create access token.
			Token lobj_AccessToken = new Token (publicToken, secretToken);
			        
			// Create, sign and send request.
			OAuthRequest lobj_Request = new OAuthRequest(Verb.GET, "http://api.telldus.com/xml/sensors/list");
			lobj_OAuthService.signRequest(lobj_AccessToken, lobj_Request);
			Response lobj_Response = lobj_Request.send();
			        
			// Print the result.
			System.out.println("Sensor list response: " + lobj_Response.getBody());

			return;
		}
		
		String ftpServer   = args[4];
		String ftpUser     = args[5];
		String ftpPassword = args[6];
		int timeInterval   = Integer.parseInt(args[7]);
		
		Vector<String> sensorList = new Vector<String>();
		
		for(int i=8; i < args.length; i++) {
			sensorList.add(args[i]);
		}
				
		while(true) {

			for(int i=0; i < sensorList.size(); i++) {
			
				//Create OAuth service.
				OAuthService lobj_OAuthService = new ServiceBuilder()
				                .provider(GoogleApi.class)                                
				                .apiKey(publicKey)                                
				                .apiSecret(privateKey)   
				                .build();

				// Create access token.
				Token lobj_AccessToken = new Token (publicToken, secretToken);
		        
				// Create, sign and send request.
				OAuthRequest lobj_Request = new OAuthRequest(Verb.GET, "http://api.telldus.com/xml/sensor/info");
				lobj_Request.addQuerystringParameter("id", sensorList.elementAt(i));
				lobj_OAuthService.signRequest(lobj_AccessToken, lobj_Request);
				Response lobj_Response = lobj_Request.send();
		        
				// Print the result.
				//System.out.println("Response: " + lobj_Response.getBody());
		
				String oneRow = "";
				
				try {
			 
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
								
					StringBufferInputStream reader = new StringBufferInputStream(lobj_Response.getBody());
				
					Document doc = dBuilder.parse(reader);
					doc.getDocumentElement().normalize();
		 
					NodeList nList = doc.getElementsByTagName("sensor");
					for (int temp = 0; temp < nList.getLength(); temp++) {
		 
						Node nNode = nList.item(temp);
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		 
							Element eElement = (Element) nNode;
							Date time = new Date();
				      
							SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy/HH/mm");
							oneRow = dateFormatter.format(time);
				      
							//System.out.println("Client Name : " + getTagValue("clientName", eElement));
							System.out.println("Sensor name : " + getTagValue("name", eElement).trim() );
							//System.out.println("Last updated : " + getTagValue("lastUpdated", eElement));
				          
							NodeList nlList = eElement.getElementsByTagName("data");
							for(int j=0; j < nlList.getLength(); j++) {
								Node nValue = (Node) nlList.item(j);
								NamedNodeMap attributes = nValue.getAttributes();
				        	  
								oneRow += "," + attributes.getNamedItem("value").getNodeValue(); 
								System.out.println(attributes.getNamedItem("name").getNodeValue().trim() + ": " + attributes.getNamedItem("value").getNodeValue().trim());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
		
				oneRow += "\n";
		 
				try {
					FileWriter file = new FileWriter(sensorList.elementAt(i) + ".txt", true);
					file.write(oneRow);
					file.close();
				} catch(Exception e) {
			
				}
		
				FTPClient client = new FTPClient();
				try {
					client.connect(ftpServer);
					client.login(ftpUser/*"telldus"*/, ftpPassword/*"Tellstick1"*/);
					client.upload(new java.io.File(sensorList.elementAt(i) + ".txt"));
					client.disconnect(true);

				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FTPIllegalReplyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FTPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FTPDataTransferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FTPAbortedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(timeInterval == 0) {
				break;
			}
			
			try {
				Thread.sleep(1000 * 60 * timeInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = (Node) nlList.item(0);
		return nValue.getNodeValue();
	}
}
