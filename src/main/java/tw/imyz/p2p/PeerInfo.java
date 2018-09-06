package tw.imyz.p2p;

import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.util.Objects;

public class PeerInfo {

    public final InetAddress address;
    public final int port;
    public final String id;

    public PeerInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.id = null;
    }

    public PeerInfo(InetAddress address, int port, String id) {
        this.address = address;
        this.port = port;
        this.id = id;
    }

    private boolean has_id() {
        return this.id != null && !this.id.isEmpty();
    }

    @Override
    public String toString() {
        return this.address.getHostAddress() + ":" + this.port + (has_id() ? " [" + this.id + "] " : "");
    }

    @Override
    public int hashCode() {
        return this.address.hashCode() + port + (this.id == null ? 0 : this.id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof PeerInfo) {
            PeerInfo other = (PeerInfo) obj;
            return (this.port == other.port) &&
                    Objects.equals(this.address, other.address) &&
                    Objects.equals(this.id, other.id);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public JSONObject to_JSON_object() {
        return new JSONObject() {{
            put(Contract.JSON_PEER_ADDRESS, PeerInfo.this.address.getHostAddress());
            put(Contract.JSON_PEER_PORT, PeerInfo.this.port);
            if (PeerInfo.this.id != null && !PeerInfo.this.id.isEmpty()) {
                put(Contract.JSON_PEER_ID, PeerInfo.this.id);
            }
        }};
    }

}
