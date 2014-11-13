Blackbuck
=========

The **easiest** game networking library in Java available! You don't have to know anything about networking, _you don't have to write boring writing/parsing & message code_, and Blackbuck will automatically handle **compressing** your data, **prioritization**, virtual **channels** of communition, and let you customize how **reliable** and **ordered** your data is!

### Features
1. `Simple`
 * messages are sent by calling methods, you don't need to create a class for every type of message communicated between server and client AND write the writing/parsing code.
2. `Fast`
 * uses non-blocking I/O
3. `Compression`
 * takes advantage of another library to store numbers in the least number of bytes possible
4. `Security`
 * packets that don't have the correct magic number for your game are ignored and the erroneous connection is closed
 * messages that are expected to be received in a certain client state but are not can result in closing the connection, logging, notification, or nothing.

### How it works

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
Protocol protocol = new Protocol( 1380, new TcpProtocolProvider() );
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

When you call something like `server.onSendMessage( "Hello, World!" )` it queues that method call on the client. The client will send method calls every so often (user specified, default 20 batches per second). Once the server receives the method call, the listener on the server side is found and the appropriate method is invoked.

### When does it send the messages?

There's a snippet of code you need to add in your update loop which handles processing messages in the queue into packets and sending them, as well as reading in messages and notifying listeners.

*Client Update Logic*
```java
client.read();
client.update();

if (client.isReadyToSend())
{
  client.send();
}
```

*Server Update Logic*
```java
while (!server.isClosed())
{
  server.update();
}
```
