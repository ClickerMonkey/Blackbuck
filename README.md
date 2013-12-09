Blackbuck
=========

A game networking library in java focused around making asynchronous method calls.

With Blackbuck you can define interfaces for one-way communication between a client and a server. 

### Defining Your Remote Interfaces 

```java
@RemoteInterface(id = 1)
public class UserToServer 
{
  @RemoteMethod(id = 1)
  public void onSendMessage( String message );
}

@RemoteInterface(id = 2)
public class ServerToUser
{
  @RemoteMethod(id = 1)
  public void onMessageReceived( String message );
}

```

### Defining the Protocol

```java
Protocol protocol = new Protocol( 1380, new TcpNetworking() );
protocol.addInterface( UserToServer.class );
protocol.addInterface( ServerToUser.clsas );
```

### Connecting to the Server
```java
// connect to server
Client client = protocol.newClient( host, port );
client.init();

// handle calls from server
protocol.addListener( new ServerToUser() {
  public void onMessageReceived( String message ) {
    System.out.println( "received: " + message );
  }
} );

// send message to server
UserToServer server = client.newService( UserToServer.class );
server.onSendMessage( "Hello, World!" );
```

### Listening for Clients
```java
// listen on port
Server server = protocol.newServer( port );
server.init();

Service<ServerToUser> users = protocol.newService( ServerToUser.class );

// handler calls from user
protocol.addListener( new UserToServer() {
  public void onSendMessage( String message ) {
    // send message to all clients connected
    users.setTarget( server.getClients() );
    users.get().onMessageReceived( message );
  }
} );
```

### How does it work?

When you call something like `server.onSendMessage( "Hello, World!" )` it queues that method call on the client. The client will send method calls every so often (user specified, default 20 batchers per second). Once the server receives the method call, the listener on the server side is found and the appropriate method is invoked.

