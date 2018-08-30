package tw.imyz.p2p;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	private final Map<String, Map<DestInfo, Long>> groups = new HashMap<>();

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

	private final byte[] buffer = new byte[1024 * 32];
    private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> sending_queue;

	public void run() {

	    if (this.socket != null) {
            Thread thread_receive_request = new Thread(Server.this::receive_request);
            Thread thread_sending_packet = new Thread(Server.this::send_notifications);
            Thread thread_update_peers_info = new Thread(Server.this::update_peers_info);

            thread_receive_request.setName("receive_request");
            thread_sending_packet.setName("sending_packet");
            thread_update_peers_info.setName("update_peers_info");
            thread_receive_request.start();
            thread_sending_packet.start();
            thread_update_peers_info.start();
            try {
                thread_receive_request.join();
                thread_sending_packet.join();
                thread_update_peers_info.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Program ended :C good bye");
        }

	}

	private void receive_request() {
	    while (!this.socket.isClosed()) {
            try {
                boolean new_peer = true;
                // receive packet
                this.socket.receive(this.packet);
                // record timestamp
                long timestamp = System.currentTimeMillis();
                // record peer info
                DestInfo peer = new DestInfo(this.packet.getAddress(), this.packet.getPort());
                // translate bytes to string
                String group_key =
                        new String(this.packet.getData(), 0, this.packet.getLength(), StandardCharsets.UTF_8);
                // synchronize before operations
                synchronized (this.groups) {
                    // add to group or create a new group
                    Map<DestInfo, Long> map = this.groups.get(group_key);
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
                        this.groups.put(group_key, map);
                    }
                }
                // print
                if (new_peer) System.out.println(String.format("received group key [%s] from %s:%d",
                        group_key, peer.address.getHostAddress(), peer.port));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void send_notifications() {
	    while (!this.socket.isClosed()) {
            try {
                DatagramPacket packet = sending_queue.take();
                this.socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private JSONArray get_peers_JSON(String group_id) {
        Map<DestInfo, Long> peers = this.groups.get(group_id);
        JSONArray peer_list = new JSONArray();
        for (Map.Entry<DestInfo, Long> peer_info : peers.entrySet()) {
            DestInfo peer = peer_info.getKey();
            JSONObject json_peer_info = new JSONObject();
            json_peer_info.put("address", peer.address.getHostAddress());
            json_peer_info.put("port", peer.port);
            peer_list.add(json_peer_info);
        }
        return peer_list;
    }

    @SuppressWarnings("unchecked")
	private void update_peers_info() {
	    while (!this.socket.isClosed()) {
	        synchronized (this.groups) {
	            Set<String> group_remove_list = new HashSet<>();
                for (Map.Entry<String, Map<DestInfo, Long>> entry : this.groups.entrySet()) {
                    boolean at_least_one = false;
                    String group_id = entry.getKey();
                    Map<DestInfo, Long> peers = entry.getValue();
                    // remove timeout peer
                    long current = System.currentTimeMillis();
                    peers.entrySet().removeIf(e -> {
                        boolean remove = current - e.getValue() > this.peer_timeout;
                        if (remove) System.out.println(String.format("removed %s:%d from group [%s]",
                                e.getKey().address.getHostAddress(), e.getKey().port, group_id));
                        return remove;
                    });

                    JSONObject group_info = new JSONObject();
                    group_info.put("group_id", group_id);

                    // add peers to JSON
                    group_info.put("peers", get_peers_JSON(group_id));

                    // to raw data
                    byte[] data = group_info.toJSONString().getBytes(StandardCharsets.UTF_8);
                    // sending group info to peers
                    for (Map.Entry<DestInfo, Long> peer_info : peers.entrySet()) {
                        at_least_one = true;
                        DestInfo peer = peer_info.getKey();
                        DatagramPacket packet = new DatagramPacket(data, data.length, peer.address, peer.port);
                        this.sending_queue.add(packet);
                    }
                    if (!at_least_one) {
                        group_remove_list.add(group_id);
                    }
                }
                for (String group : group_remove_list) {
                    this.groups.remove(group);
                }
            }
            try { Thread.sleep(500); } catch (Exception ignore) { }
        }
    }

}
