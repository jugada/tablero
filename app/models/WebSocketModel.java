package models;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import play.Logger;
import play.mvc.*;
import play.libs.*;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import akka.actor.*;

public class WebSocketModel extends UntypedActor {
	
	// Default actor.
    static ActorRef defaultSpace = Akka.system().actorOf(new Props(WebSocketModel.class));
    
	// players connected
	static Map<String, WebSocket.Out<JsonNode>> connected = new HashMap<String, WebSocket.Out<JsonNode>>();
		
	// everytime a new websocket is opened we call this method to initializate the connection
	public static void connect(final String user, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
		
		// check if the player is already connected
		if(connected.containsKey(user)) {
			
			try {
				// create a json object to return the error. Is this the best way to do it?
				// if there is no easier way to create an object move this code to a custom class
				// so we can reuse it.
    			ObjectMapper mapper = new ObjectMapper();
    	        JsonFactory factory = mapper.getJsonFactory();
    	        JsonParser jp;
    	        
    	        jp = factory.createJsonParser("{\"error\":\"player already exists\"}");
    	        JsonNode actualObj;
    			actualObj = mapper.readTree(jp);
    			out.write(actualObj);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
			
		} else {
			
			// all good, tell the actor that we want to join
			defaultSpace.tell(new Join(user, out), null);
			
			// set the listener to websocket messages
	        in.onMessage(new Callback<JsonNode>() {
	           public void invoke(JsonNode event) throws JsonParseException, IOException {
	        	   
	        	   // let the actor know we received a websocket message from a player
	               defaultSpace.tell(new Message(user, event), null);
	               
	           }
	           
	        });
	        
	        // When the socket is closed.
	        in.onClose(new Callback0() {
	           public void invoke() {
	               
	               // remove this dude
	        	   connected.remove(user);
	        	   Logger.info("DISCONNECTED user "+user);
	               
	           }
	        });
	        
		}
	}

	// this is where the magic happens!
	@Override
	public void onReceive(Object message) throws Exception {
		// TODO Auto-generated method stub
		if(message instanceof Join) {
            
            // Received a Join message
            Join join = (Join)message;
            connected.put(join.user, join.out);
            
            // create the character
            if (!join.user.equals("TABLERO")){
            	ObjectMapper mapper = new ObjectMapper();
    	        JsonFactory factory = mapper.getJsonFactory();
    	        JsonParser jp;
    	        
    	        jp = factory.createJsonParser("{\"action\":\"connect\",\"player\":\""+join.user+"\"}");
    	        JsonNode actualObj;
    			actualObj = mapper.readTree(jp);
            	connected.get("TABLERO").write(actualObj);
            }
            
        }
		else if (message instanceof Message){
			
			// Received a message. Do something with it depending on the content of the json object
            Message mess = (Message)message;
			Logger.info("Received: "+mess.message.toString()+ " FROM: "+mess.user);	
			
			ObjectMapper mapper = new ObjectMapper();
	        JsonFactory factory = mapper.getJsonFactory();
	        JsonParser jp;
	        
	        jp = factory.createJsonParser("{\"action\":\"control\",\"player\":\""+mess.user+"\",\"move\":\""+mess.message.get("button").asText()+"\"}");
	        JsonNode actualObj;
			actualObj = mapper.readTree(jp);
			// echo back the object, this reduces latency on ios
            connected.get(mess.user).write(actualObj);
            connected.get("TABLERO").write(actualObj);
					
		}
		
	}
	
	// MESSAGES DEFINITIONS
	
    public static class Join {
        
        final String user;
        final WebSocket.Out<JsonNode> out;
        
        public Join(String user, WebSocket.Out<JsonNode> out) {
            this.user = user;
            this.out = out;
        }
        
    }
    
    public static class Message {
        
        final String user;
        final JsonNode message;
        
        public Message(String user, JsonNode message) {
            this.user = user;
            this.message = message;
        }
        
    }
    
}
