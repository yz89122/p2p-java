# Intro

A P2P example made with Java DatagramSocket, uses a extra server for UDP hole punching

# Run

## Peer

run with command

```
java Peer <Server>
```

### Example

if the server address is `hello.noip.me`

```
java Peer hello.noip.me
```

## Server

the default listening port is `5555`

```
java Server [listening_port]
```

### Example

```
java Server
```

