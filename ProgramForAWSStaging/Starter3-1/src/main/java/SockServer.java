import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.*;
import java.io.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;

  public static void main (String args[]) {

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and calling handling one request.
       *
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s);

          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("addmany")) {
            res = addmany(req);
          } else if (req.getString("type").equals("charcount")) {
            res = charCount(req);
          } else if (req.getString("type").equals("inventory")) {
            res = inventory(req);
          } else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  // implement me in assignment 3
  static JSONObject inventory(JSONObject req) {
    JSONObject res = new JSONObject();
    System.out.println("inventory request: " + req.toString());

    JSONObject validate = testField(req, "task");
    if (!validate.getBoolean("ok")) {
      JSONObject errorResponse = new JSONObject();
      errorResponse.put("ok", false);
      errorResponse.put("type", "inventory");
      errorResponse.put("message", "Invalid task");

    }

    String tasktype = validate.getString("task").toLowerCase();// ensure that all sting is lower case

    switch (tasktype){
      case "add":
          res = updateInventory( req);
        break;
      case "view":

        break;
      case "buy":
        break;
    }

    return res;
  }

  // implement me in assignment 3
  static JSONObject charCount(JSONObject req) {
    JSONObject res = new JSONObject();
    System.out.println("Character count request: " + req.toString());

    // Validate the "count" field
    JSONObject validation = testField(req, "count");
    if (!validation.getBoolean("ok")) {
      JSONObject errorResponse = new JSONObject();
      errorResponse.put("type", "charcount");
      errorResponse.put("ok", false);
      errorResponse.put("message", "Field 'count' needs to be of type: string");
      return errorResponse;
    }

    String inputString = req.getString("count");
    int results = 0;

    // Case 1: General character count (findchar = false)
    if (!req.getBoolean("findchar")) {
      results = inputString.length();
      res.put("type", "charcount");
      res.put("ok", true);
      res.put("result", results);
      return res;
    }

    // Case 2: Specific character count (findchar = true)
    if (!req.has("find")) {
      JSONObject errorResponse = new JSONObject();
      errorResponse.put("type", "charcount");
      errorResponse.put("ok", false);
      errorResponse.put("message", "Field 'find' is missing.");
      return errorResponse;
    }

    String findString = req.getString("find");

    if (findString.isEmpty()) {
      JSONObject errorResponse = new JSONObject();
      errorResponse.put("type", "charcount");
      errorResponse.put("ok", false);
      errorResponse.put("message", "Field 'find' cannot be empty.");
      return errorResponse;
    }

    if (findString.length() != 1) {
      JSONObject errorResponse = new JSONObject();
      errorResponse.put("type", "charcount");
      errorResponse.put("ok", false);
      errorResponse.put("message", "Field 'find' must be exactly one character.");
      return errorResponse;
    }

    char character = findString.charAt(0);

    for (char c : inputString.toCharArray()) {
      if (c == character) {
        results++;
      }
    }

    res.put("type", "charcount");
    res.put("ok", true);
    res.put("result", results);

    return res;
  }


  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    JSONArray array = req.getJSONArray("nums");
    for (int i = 0; i < array.length(); i ++){
      try{
        result += array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }

  static JSONObject updateInventory(JSONObject req) {
    ObjectMapper objectMapper = new ObjectMapper();
    File file = new File("resources/inventory.json");
    JSONObject response = new JSONObject(); // Response JSON object

    response.put("type", "inventory");

    try {
      JsonNode root;
      ArrayNode inventory;

      // Check if file exists and read it, otherwise create new structure
      if (file.exists()) {
        root = objectMapper.readTree(file);
        inventory = (ArrayNode) root.get("inventory");
      } else {
        root = objectMapper.createObjectNode();
        inventory = objectMapper.createArrayNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("inventory", inventory);
      }

      // Check if the request contains "inventory" array
      if (!req.has("inventory")) {
        response.put("ok", false);
        response.put("message", "Missing 'inventory' field in request.");
        return response;
      }

      // Extract inventory array from request
      JSONArray inventoryArray = req.getJSONArray("inventory");

      // Append each item from request to the inventory list
      for (int i = 0; i < inventoryArray.length(); i++) {
        JSONObject item = inventoryArray.getJSONObject(i);

        // Validate required fields
        if (!item.has("product") || !item.has("quantity")) {
          response.put("ok", false);
          response.put("message", "Each inventory item must have 'product' and 'quantity'.");
          return response;
        }

        // Convert JSONObject to Jackson JsonNode
        JsonNode newItem = objectMapper.readTree(item.toString());

        // Add new item to inventory
        inventory.add(newItem);
      }

      // Write updated JSON back to file
      objectMapper.writeValue(file, root);
      response.put("ok", true);
      response.put("message", "Inventory updated successfully!");

    } catch (IOException e) {
      response.put("ok", false);
      response.put("message", "Error writing to file: inventory.json.");
      e.printStackTrace();
    }

    return response;
  }

  public static JSONObject viewInventory() {
    ObjectMapper objectMapper = new ObjectMapper();
    File file = new File("resources/inventory.json");
    JSONObject response = new JSONObject(); // Response JSON object

    response.put("type", "inventory");

    try {
      // Check if the file exists
      if (!file.exists()) {
        response.put("ok", false);
        response.put("message", "Inventory file not found.");
        return response;
      }

      // Read the JSON file
      JsonNode root = objectMapper.readTree(file);
      ArrayNode inventory = (ArrayNode) root.get("inventory");

      // Check if inventory exists and is not empty
      if (inventory == null || inventory.isEmpty()) {
        response.put("ok", false);
        response.put("message", "Inventory is empty.");
        return response;
      }

      // Add inventory data to the response
      response.put("ok", true);
      response.put("inventory", inventory.toString()); // Convert ArrayNode to JSON String

    } catch (IOException e) {
      response.put("ok", false);
      response.put("message", "Error reading inventory file.");
      e.printStackTrace();
    }

    return response;
  }

}