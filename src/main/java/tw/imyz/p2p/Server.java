package tw.imyz.p2p;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import tw.imyz.util.JSON;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {

    private static final int DEFAULT_LISTEN_PORT = 5555;
    private static final long DEFAULT_PEER_TIMEOUT = 5000;

	public static void main(String args[]) {
	    int listening_port = DEFAULT_LISTEN_PORT;
	    long timeout = DEFAULT_PEER_TIMEOUT;

	    if (args.length >= 1) {
            try {
                listening_port = Integer.valueOf(args[0]);
            } catch (Exception e) {
                System.err.println("run with command 'java Server [listening_port [timeout]]'");
                System.err.println("or prefix 'java -jar Server.jar' if you're using jar file");
                System.err.println("default listening_port is " + DEFAULT_LISTEN_PORT);
                e.printStackTrace();
                System.exit(-1);
            }
        }

        if (args.length >= 2) {
            try {
                timeout = Integer.valueOf(args[1]);
            } catch (Exception e) {
                System.err.println("run with command 'java Server [listening_port [timeout]]'");
                System.err.println("or prefix 'java -jar Server.jar' if you're using jar file");
                System.err.println("default timeout is " + DEFAULT_PEER_TIMEOUT);
                e.printStackTrace();
                System.exit(-1);
            }
        }

		Server server = new Server(listening_port, timeout);
		server.run(); 
	}

//    private final int listening_port;
    private final long peer_timeout;
	private final Map<String, Map<PeerInfo, Long>> groups = new HashMap<>();

    public Server(int listening_port, long peer_timeout) {
//		this.listening_port = listening_port;
		// in order to suppress warning :C
        this.peer_timeout = peer_timeout;
		DatagramSocket socket = null;
		try {
            socket = new DatagramSocket(listening_port);
            System.out.println(String.format("Server listening on port [%d]", listening_port));
        } catch (Exception e) {
            System.err.println(";C");
            e.printStackTrace();
        }
        this.socket = socket;
		this.sending_queue = new ArrayBlockingQueue<>(1 << 16);
	}

	private final byte[] buffer = new byte[1 << 16];
    private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> sending_queue;

	public void run() {

	    if (this.socket != null) {
            Thread thread_receiving = new Thread(Server.this::thread_receiving);
            Thread thread_sending = new Thread(Server.this::thread_sending);
            Thread thread_update_peers_info = new Thread(Server.this::update_peers_info);

            thread_receiving.setName("thread_receiving");
            thread_sending.setName("thread_sending");
            thread_update_peers_info.setName("update_peers_info");
            thread_receiving.start();
            thread_sending.start();
            thread_update_peers_info.start();

            try {
                thread_receiving.join();
                thread_sending.join();
                thread_update_peers_info.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Program ended :C good bye");
        }

	}

	@SuppressWarnings("unchecked")
	private void thread_receiving() {
	    while (!this.socket.isClosed()) {
            try {
                boolean new_peer = true;
                // receive packet
                this.socket.receive(this.packet);
                // record timestamp
                long timestamp = System.currentTimeMillis();
                // record peer info
                PeerInfo peer = new PeerInfo(this.packet.getAddress(), this.packet.getPort());
                // translate bytes to string
                String message =
                        new String(this.packet.getData(), 0, this.packet.getLength(), StandardCharsets.UTF_8);

                String group_id = message;
                try {
                    JSONObject json_object = JSON.parse(message);
                    group_id = (String) json_object.getOrDefault(Contract.JSON_GROUP_ID, "default");
                    String peer_id = (String) json_object.get(Contract.JSON_PEER_ID);
                    peer = new PeerInfo(this.packet.getAddress(), this.packet.getPort(), peer_id);
                } catch (ParseException e) {
                    System.err.println(String.format("failed to parse message to JSON from %s:%d",
                            this.packet.getAddress().getHostAddress(), this.packet.getPort()));
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("Unknown error");
                    e.printStackTrace();
                }

                // synchronize before operations
                synchronized (this.groups) {
                    // add to group or create a new group
                    Map<PeerInfo, Long> map = this.groups.get(group_id);
                    // if the group doesn't exist
                    if (map != null) {
                        if (map.containsKey(peer)) {
                            new_peer = false;
                        }
                        // add peer to group
                        map.put(peer, timestamp);
                    } else {
                        // create a new group
                        map = new HashMap<>();
                        map.put(peer, timestamp);
                        // put group
                        this.groups.put(group_id, map);
                    }
                }
                // print
                if (new_peer) System.out.println(String.format("received group key [%s] from %s:%d",
                        group_id, peer.address.getHostAddress(), peer.port));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void thread_sending() {
	    while (!this.socket.isClosed()) {
            try {
                this.socket.send(sending_queue.take());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONArray get_peers_JSON(Iterable<PeerInfo> peers) {
        JSONArray peer_list = new JSONArray();
        for (PeerInfo peer : peers) {
            peer_list.add(peer.to_JSON_object());
        }
        return peer_list;
    }

    @SuppressWarnings("unchecked")
	private void update_peers_info() {
	    while (!this.socket.isClosed()) {
	        synchronized (this.groups) {
                for (Map.Entry<String, Map<PeerInfo, Long>> entry : this.groups.entrySet()) {
                    String group_id = entry.getKey();
                    Map<PeerInfo, Long> peers = entry.getValue();

                    // remove timeout peer
                    long current = System.currentTimeMillis();
                    peers.entrySet().removeIf(e -> {
                        boolean remove = current - e.getValue() > this.peer_timeout;
                        if (remove) System.out.println(String.format("removed %s:%d from group [%s]",
                                e.getKey().address.getHostAddress(), e.getKey().port, group_id));
                        return remove;
                    });

                    JSONObject group_info = new JSONObject();
                    group_info.put(Contract.JSON_GROUP_ID, group_id);

                    Set<PeerInfo> peers_info = new HashSet<>(peers.keySet());

                    // sending group info to peers
                    for (PeerInfo peer : peers.keySet()) {
                        // remove the info of the target peer before sending them the group info
                        // this prevent them from sending message to themselves

                        // remove the target self from the set
                        peers_info.remove(peer);
                        // add peers info to JSON object
                        group_info.put(Contract.JSON_PEERS, get_peers_JSON(peers_info));

                        // to raw data
                        byte[] data = group_info.toJSONString().getBytes(StandardCharsets.UTF_8);

                        // remove
                        group_info.remove(Contract.JSON_PEERS);
                        // add it back
                        peers_info.add(peer);

                        // make a packet
                        DatagramPacket packet = new DatagramPacket(data, data.length, peer.address, peer.port);
                        // append the packet to sending queue
                        this.sending_queue.add(packet);
                    }
                }
                // remove the group if the group is empty
                this.groups.entrySet().removeIf(group -> group.getValue().isEmpty());
            }
            try { Thread.sleep(500); } catch (Exception ignore) { }
        }
    }

}
