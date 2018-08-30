package tw.imyz.p2p;

import java.net.InetAddress;

public class DestInfo {

    public final InetAddress address;
    public final int port;

    public DestInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode() + port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DestInfo) {
            DestInfo other = (DestInfo) obj;
            return (this.port == other.port) &&
                    (this.address == other.address ||
                            (this.address != null && this.address.equals(other.address)));
        }
        return false;
    }

}
