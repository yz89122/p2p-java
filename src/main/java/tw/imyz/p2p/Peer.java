package tw.imyz.p2p;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import tw.imyz.random_name.RandomName;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Peer {

    private static final int DEFAULT_SERVER_PORT = 5555;

    private static final String[] BAD_THINGS_HAPPENED = {";(", ";C", "QQ", "QAQ", "( ; _ ; )/~~~"};
    private static final String[] GOOD_THINGS_HAPPENED = {":)", ":D", "XD", "(´･ω･`)", "(´･ω･`)"};

    private static final String[] MESSAGE_TEMPLATE = {
            "sending UDP packets to [${host}:${listening_port] ${emoji}",
            "sending bombs to [${host}:${listening_port}] ${emoji}"
    };

    private static final Random rand = new Random();

    private static String bad() {
        return BAD_THINGS_HAPPENED[rand.nextInt(BAD_THINGS_HAPPENED.length)];
    }

    private static String good() {
        return GOOD_THINGS_HAPPENED[rand.nextInt(GOOD_THINGS_HAPPENED.length)];
    }

    private final InetAddress server_address;
    private final int server_port;
//    private final String group_key;
    private final byte[] bytes_group_id;
    private final String identifier;
    private final DatagramSocket socket;
    private final byte[] buffer = new byte[1024 * 32];
    private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    public Peer(InetAddress server_address, int server_port, String group_id, String identifier) throws SocketException {
        this.server_address = server_address;
        this.server_port = server_port;
//        this.group_id = group_id;
        this.bytes_group_id = group_id.getBytes(StandardCharsets.UTF_8);
        this.identifier = identifier;
        socket = new DatagramSocket();
    }

    private DestInfo[] peers;

    private void update_peers(DestInfo[] peers) {
        this.peers = peers;
    }

    private DestInfo[] get_peers() {
        return this.peers;
    }

    public void run() {
        System.out.println(String.format("start with identifier [%s]", this.identifier));

        Thread register_self_to_server = new Thread(Peer.this::register);
        Thread thread_receive_msg = new Thread(Peer.this::receive_msg);
        Thread thread_send_shit_to_peers = new Thread(Peer.this::send_shit_to_peers);
        register_self_to_server.start();
        thread_receive_msg.start();
        thread_send_shit_to_peers.start();

        try {
            register_self_to_server.join();
            thread_receive_msg.join();
            thread_send_shit_to_peers.join();
        } catch (Exception ignore) {
        }
    }

    private void receive_msg() {
        while (!this.socket.isClosed()) {
            try {
                this.socket.receive(this.packet);
                String msg = new String(
                        this.packet.getData(), 0, this.packet.getLength(), StandardCharsets.UTF_8);
                if (this.packet.getAddress().equals(this.server_address) && this.packet.getPort() == this.server_port) {
                    System.out.println("Server:" + ":" + this.packet.getPort() + " '" + msg + "'");
                    JSONObject group_info = (JSONObject) (new JSONParser()).parse(msg);
                    JSONArray json_peers = (JSONArray) group_info.get("peers");
                    DestInfo[] peers = new DestInfo[json_peers.size()];
                    for (int i = 0; i < peers.length; ++i) {
                        JSONObject json_peer = (JSONObject) json_peers.get(i);
                        String str_address = (String) json_peer.get("address");
                        int port = (int) (long) json_peer.get("port");
                        InetAddress address = InetAddress.getByName(str_address);
                        peers[i] = new DestInfo(address, port);
                    }
                    update_peers(peers);
                } else {
                    System.out.println(this.packet.getAddress().getHostAddress() +
                            ":" + this.packet.getPort() + " '" + msg + "'");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void send_shit_to_peers() {
        while (!this.socket.isClosed()) {
            DestInfo[] peers = get_peers();
            if (peers != null) {
                for (DestInfo peer : peers) {
                    byte[] data = ("Hi! I'm " + this.identifier).getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(data, data.length, peer.address, peer.port);
                    try {
                        this.socket.send(packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            try { Thread.sleep(500); } catch (Exception ignore) { }
        }
    }

    @SuppressWarnings("unchecked")
    private void register() {
        while (!this.socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(this.bytes_group_id, this.bytes_group_id.length, this.server_address, this.server_port);
            try {
                this.socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-4);
            }
            try { Thread.sleep(500); } catch (InterruptedException ignore) { }
        }
    }

	public static void main(String args[]) {

        String server_host;
        int server_port = DEFAULT_SERVER_PORT;
        String group_id = "default";

        if (args.length < 1) {
            System.err.println("run with command 'java Peer <server_host>[:listening_port] [group_id]' " + bad());
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
