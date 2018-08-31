# Intro

A P2P example made with Java DatagramSocket.

## How it works?

It uses a third party to tell peers what their public IP and port is(which is used for send message to the Server), then the peers will try to connect to each other.

And this technique is so called 'hole punching'.

It's not 100% works, depends on the NAT types.

# How to build?

Use gradle wrapper to build executable Jar file, then the `Peer.jar` and the `Server.jar` will be located at directory `./build/jar`

```
./gradlew build
```

or if you're using Windows

```
gradlew.bat build
```

(Actually, I'm not sure about this, cause my laptop is running Linux :D. I like Linux, BTW.)

# Run

## Peer

Run with command.

```
java -jar Peer.jar <Server>[:port] [group_id]
```

The default `group_id` is `default`.

### Example

If the server address is `hello.noip.me`.

```
java -jar Peer.jar hello.noip.me
```

## Server

The default listening port is `5555`.

```
java -jar Server.jar [listening_port]
```

### Example

```
java -jar Server.jar
```

# How to know is it works?

If successfully receive message from server, it'll output something like

```
Server '{"group_id":"default","peers":[{"address":"1.2.3.4","port":56789}]}'
```

or more peers connected to the same server and use same `group_id`

```
Server '{"group_id":"default","peers":[{"address":"1.2.3.4","port":56789},{"address":"5.6.7.8","port":56790}]}'
```

If successfully message from other peers, it'll output something like

```
1.2.3.4:56789 'Hi! I'm musing_lalande'
```
