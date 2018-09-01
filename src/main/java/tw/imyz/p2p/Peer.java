package tw.imyz.p2p;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import tw.imyz.asciiart.Faces;
import tw.imyz.random_name.RandomName;
import tw.imyz.util.EmptyCollection;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Peer {

    private static final int DEFAULT_SERVER_PORT = 5555;

    private static final String[] MESSAGE_TEMPLATE = {
            "sending UDP packets to [${host}:${listening_port] ${asciiart}",
            "sending bombs to [${host}:${listening_port}] ${asciiart}"
    };

    private static final Random rand = new Random();

    private final InetAddress server_address;
    private final int server_port;
    private final String group_id;
    private final DatagramPacket register_packet;
    private final String identifier;
    private final DatagramSocket socket;
    private final byte[] buffer = new byte[1 << 16];
    private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    private final BlockingQueue<DatagramPacket> sending_queue = new ArrayBlockingQueue<>(1 << 12);

    @SuppressWarnings("unchecked")
    public Peer(InetAddress server_address, int server_port, String group_id, String identifier) throws SocketException {
        this.server_address = server_address;
        this.server_port = server_port;
        this.group_id = group_id;
        this.identifier = identifier;

        byte[] tmp = (new JSONObject() {{
            put(Contract.JSON_GROUP_ID, Peer.this.group_id);
            put(Contract.JSON_PEER_ID, Peer.this.identifier);
        }}).toJSONString().getBytes(StandardCharsets.UTF_8);
        this.register_packet = new DatagramPacket(tmp, tmp.length, this.server_address, this.server_port);
        socket = new DatagramSocket();
    }

    private Collection<PeerInfo> peers;

    private Collection<PeerInfo> get_peers() {
        return this.peers != null ? this.peers : EmptyCollection.collection;
    }

    private void update_peers(Collection<PeerInfo> peers) {
        Set<PeerInfo> new_peers = new HashSet<>(peers);
        new_peers.removeAll(get_peers());
        for (PeerInfo peer : new_peers) {
            System.out.println(peer.toString() + " joined");
        }
        Set<PeerInfo> left_peers = new HashSet<>(get_peers());
        left_peers.removeAll(peers);
        for (PeerInfo peer : left_peers) {
            System.out.println(peer.toString() + " left");
        }
        this.peers = peers;
    }

    public void run() {
        System.out.println(String.format("start with identifier [%s]", this.identifier));

        Thread thread_register = new Thread(Peer.this::thread_register);
        Thread thread_receiving = new Thread(Peer.this::thread_receiving);
        Thread thread_sending = new Thread(Peer.this::thread_sending);
        Thread thread_broadcast_to_peers = new Thread(Peer.this::thread_broadcast_to_peers);

        thread_register.setName("thread_register");
        thread_receiving.setName("thread_receiving");
        thread_sending.setName("thread_sending");
        thread_broadcast_to_peers.setName("thread_broadcast_to_peers");

        thread_register.start();
        thread_receiving.start();
        thread_sending.start();
        thread_broadcast_to_peers.start();

        try {
            thread_register.join();
            thread_receiving.join();
            thread_sending.join();
            thread_broadcast_to_peers.join();
        } catch (Exception ignore) { }
    }

    @SuppressWarnings("unckecked")
    private void thread_receiving() {
        while (!this.socket.isClosed()) {
            try {
                this.socket.receive(this.packet);
                String msg = new String(
                        this.packet.getData(), 0, this.packet.getLength(), StandardCharsets.UTF_8);

                // if the message is from the server
                if (this.packet.getAddress().equals(this.server_address) && this.packet.getPort() == this.server_port) {
                    try {
                        JSONObject group_info = (JSONObject) (new JSONParser()).parse(msg);
                        JSONArray json_peers = (JSONArray) group_info.get("peers");
                        HashSet<PeerInfo> peers = new HashSet<>();
                        for (Object object : json_peers) {
                            try {
                                JSONObject json_peer = (JSONObject) object;
                                String peer_address_str = json_peer.get(Contract.JSON_PEER_ADDRESS).toString();
                                InetAddress peer_address = InetAddress.getByName(peer_address_str);
                                int port = (int) (long) json_peer.get(Contract.JSON_PEER_PORT);
                                peers.add(new PeerInfo(peer_address, port));
                            } catch (Exception ignore) { }
                        }
                        update_peers(peers);
                    } catch (ParseException e) {
                        System.out.println("Server: '" + msg + "'");
                        System.err.println("failed to parse message from the server " + Faces.bad());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println(this.packet.getAddress().getHostAddress() +
                            ":" + this.packet.getPort() + " '" + msg + "'");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void thread_sending() {
        while (!this.socket.isClosed()) {
            try {
                this.socket.send(this.sending_queue.take());
            } catch (IOException e) {
                System.err.println("failed to send packet");
                System.err.println("exit now");
                e.printStackTrace();
                System.exit(-3);
            } catch (InterruptedException ignore) { }
        }
    }

    @SuppressWarnings("unchecked")
    private void thread_register() {
        while (!this.socket.isClosed()) {
            this.sending_queue.add(this.register_packet);
            try { Thread.sleep(500); } catch (InterruptedException ignore) { }
        }
    }

    private void thread_broadcast_to_peers() {
        Scanner stdin = new Scanner(System.in);
        while (!this.socket.isClosed()) {
            byte[] data = stdin.nextLine().getBytes(StandardCharsets.UTF_8);
            for (PeerInfo peer : get_peers()) {
                DatagramPacket packet = new DatagramPacket(data, data.length, peer.address, peer.port);
                this.sending_queue.add(packet);
            }
        }
    }

	public static void main(String args[]) {

        String server_host;
        int server_port = DEFAULT_SERVER_PORT;
        String group_id = "default";

        if (args.length < 1) {
            System.err.println("run with command 'java Peer <server_host>[:listening_port] [group_id]' " + Faces.bad());
            System.exit(-1);
        }

        String[] server = args[0].split(":");
        server_host = server[0];
        try { server_port = Integer.valueOf(server[1]); } catch (Exception ignore) { }
        try { group_id = args[1]; } catch (Exception ignore) { }

        try {
            (new Peer(InetAddress.getByName(server_host), server_port, group_id, RandomName.pick())).run();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-2);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(-3);
        }

	}

}
