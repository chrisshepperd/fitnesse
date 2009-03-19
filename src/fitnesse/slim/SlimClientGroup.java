package fitnesse.slim;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlimClientGroup {
  private Map<String, SlimClient> slimClients;
  private String classPath;
  private String className;

  public SlimClientGroup() {
    slimClients = new HashMap<String, SlimClient>();
  }

  public void addSlimClient(String portType, SlimClient slimClient) {
    slimClients.put(portType.toUpperCase(), slimClient);
  }

  public Map<String, SlimClient> getSlimClients() {
    return slimClients;
  }

  public void close() throws Exception {
    for (SlimClient slimClient : slimClients.values()) {
      slimClient.close();
    }
  }

  public void connect() throws Exception {
    for (SlimClient slimClient : slimClients.values()) {
      if (!slimClient.isConnected())
        slimClient.connect();
    }
  }

  public String getVersion() {
    String version = "";

    for (SlimClient slimClient : slimClients.values()) {
      version = slimClient.getVersion();
    }

    return version;
  }

  public boolean isConnected() {
    boolean connected = true;
    for (SlimClient slimClient : slimClients.values()) {
      connected = connected && slimClient.isConnected();
    }
    return connected;
  }

  public Map<String, Object> invokeAndGetResponse(String portType, List<Object> statements) throws Exception {
    return slimClients.get(portType.toUpperCase()).invokeAndGetResponse(statements);
  }

  public void sendBye() throws IOException {
    for (SlimClient slimClient : slimClients.values()) {
      slimClient.sendBye();
    }
  }

//  public static Map<String, Object> resultToMap(List<Object> slimResults) {
//    Map<String, Object> map = new HashMap<String, Object>();
//    for (Object aResult : slimResults) {
//      List<Object> resultList = ListUtility.uncheckedCast(Object.class, aResult);
//      map.put((String) resultList.get(0), resultList.get(1));
//    }
//    return map;
//  }

  public boolean hasPortType(String portType) {
    return slimClients.containsKey(portType.toUpperCase());
  }

  public String getClassPath() {
    return classPath;
  }

  public void setClassPath(String classPath) {
    this.classPath = classPath;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }
}