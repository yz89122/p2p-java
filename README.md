# Intro

A P2P example made with Java DatagramSocket, uses a extra server for UDP hole punching.

## What is hole punching?

It's a technique allows two peers communicate directly if they're under different NATs(different local area networks).

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

run with command

```
java -jar Peer.jar <Server>[:port] [group_id]
```

the default `group_id` is `default`

### Example

if the server address is `hello.noip.me`

```
java -jar Peer.jar hello.noip.me
```

## Server

the default listening port is `5555`

```
java -jar Server.jar [listening_port]
```

### Example

```
java -jar Server.jar
```

