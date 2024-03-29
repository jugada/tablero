package controllers;

import models.WebSocketModel;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.mvc.*;

public class Application extends Controller {
  
    public static Result index() {
    	Logger.info("APP STARTED ");
        return ok("Game engine on.");
    }
    
    
    /**
     * Websocket imlementatioon
     * 
     * Our sockets will transport JSON messages
     * 
     * @return
     */
    public static WebSocket<JsonNode> console(final String user) {
    	
        return new WebSocket<JsonNode>() {
        	
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {

            	try{
            		
            		WebSocketModel.connect(user, in, out);
            		Logger.info("CONNECTED user "+user);
            		
            	}catch (Exception e){
            		
            		Logger.info("ERROR ON SOCKET CONNECTION");
            		e.printStackTrace();
            		
            	}
            }
        };
    }
  
}
